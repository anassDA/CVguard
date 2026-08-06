# CVGuard

**Détection et neutralisation des attaques par injection de prompt dans les documents de candidature.**

Micro-service d'inspection placé en amont de tout appel à un modèle de langage. Il détecte, qualifie et
neutralise les instructions dissimulées dans un CV au format PDF ou DOCX.

Référentiel : **OWASP Top 10 for LLM Applications — LLM01 : Prompt Injection**.

> Projet personnel — Anass DADA, élève-ingénieur à l'EMSI Rabat (Ingénierie Informatique et Réseaux).
> Démonstrateur technique : le service établit la faisabilité de la contre-mesure, il n'a pas vocation à
> être déployé en production en l'état.

---

## 1. Problématique

Les systèmes de présélection automatisée (ATS) transmettent le texte d'un CV à un modèle de langage,
accompagné d'une consigne d'évaluation. Or un modèle ne dispose d'aucune séparation structurelle entre les
**instructions** qu'il reçoit et les **données** qu'il traite : tout lui parvient sous la forme d'une unique
séquence de texte.

Un candidat peut donc dissimuler dans son CV une phrase à la forme impérative — invisible à l'œil humain,
mais parfaitement extraite par le parseur — que le modèle interprétera comme une consigne légitime.

Vecteurs de dissimulation couverts :

| Vecteur | Règle |
|---|---|
| Texte de couleur identique ou très proche du fond | R2 |
| Corps typographique minuscule (< 3 pt) | R1 |
| Texte hors zone d'impression, sous une image ou derrière un aplat opaque | R3 |
| Caractères invisibles ou de largeur nulle (U+200B, U+200E, U+FEFF) | R4 |
| Métadonnées du document (Title, Subject, Keywords) et commentaires | R5 |
| Écart entre le texte extrait et le texte réellement visible (OCR) | R6 |
| Segments rédigés sous forme d'instruction adressée à un système | R7 |

Deux vecteurs supplémentaires, non listés explicitement au chapitre 1 du cahier mais identifiés durant
l'implémentation, sont couverts par la même logique que le contraste (R2) : un texte marqué invisible ne
produit à l'écran aucun contraste avec son fond, quelle que soit sa couleur déclarée.

| Vecteur natif du format | Mécanisme | Détecté via |
|---|---|---|
| Texte masqué (Word, Format > Police > Masqué) | `w:vanish` (DOCX) | `TextFragment.hidden` → R2 |
| Mode de rendu de texte invisible | `Tr 3` (PDF) — légitime dans une couche OCR de PDF scanné | `TextFragment.hidden` → R2 |

---

## 2. Architecture

Architecture hexagonale simplifiée : le moteur de détection constitue le domaine métier, isolé des
dépendances techniques par des interfaces.

```
ma.emsi.cvguard
├── exposition/                Contrôleurs REST · validation · gestion des erreurs · OpenAPI
├── application/                Orchestrateur d'inspection · assembleur de rapport
├── domain/
│   ├── model/                  Fragments, couleurs, boîtes englobantes, détections, verdicts
│   ├── rule/                   Contrat des règles · moteur d'orchestration · seuils
│   ├── score/                  Agrégation pondérée et verdict
│   ├── text/                   Normalisation NFKC · relevé des caractères invisibles (pur, sans I/O)
│   └── port/                   Extraction · rendu · OCR · client LLM
├── infrastructure/
│   └── extraction/
│       ├── UnifiedDocumentExtractor   point d'entrée unique, dispatch par format
│       ├── pdf/                 PDFBox — RenderAwareTextStripper
│       └── docx/                Apache POI — XWPF
└── config/                     Propriétés, registre des règles, assemblage du domaine
```

Le domaine ne dépend ni de Spring ni d'une bibliothèque d'extraction : `RuleEngine` et `ScoreAggregator`
sont des classes ordinaires, assemblées dans `config/DomainConfiguration`. Le moteur de règles est donc
testable sans démarrer de contexte applicatif ni ouvrir de fichier PDF.

### Chaîne de traitement

```
Document soumis
   → [1] EXTRACTION        PDFBox / POI → fragments + attributs de rendu
   → [2] NORMALISATION     Unicode NFKC, relevé des caractères invisibles
   → [3] MOTEUR DE RÈGLES  R1 corps · R2 contraste · R3 position · R4 invisibles · R5 métadonnées
        └─ signal levé ? → [4] CONTRÔLE OCR (rendu → Tesseract → écart de similarité)
   → [5] ANALYSE D'INSTRUCTION  motifs impératifs, marqueurs de rôle, balises de fin de consigne
   → [6] AGRÉGATION        score pondéré → SAFE | SUSPECT | MALICIOUS
   → [7] ASSAINISSEMENT    purge des segments + encapsulation en donnée
Rapport JSON + contenu assaini → consommateur (ATS / LLM)
```

### Calcul du score

```
score = min(100, Σ  poids(R) × (1 + log2(occurrences(R))))

score <  25  →  SAFE
score <  60  →  SUSPECT     (revue humaine requise)
score >= 60  →  MALICIOUS   (rejet du document)
```

La croissance **logarithmique** est délibérée : une règle déclenchée dix fois ne pèse pas dix fois son
poids. Un CV comportant de nombreuses mentions légales en petits caractères ne doit pas être condamné par
la répétition d'un signal faible. C'est la **concordance de règles différentes** qui fait monter le score.

Le palier `SUSPECT` matérialise le maintien de l'humain dans la boucle : le système ne prononce jamais seul
l'écartement d'une candidature dans la zone d'incertitude.

---

## 3. Pile technique

| Brique | Choix |
|---|---|
| Langage | Java 21 (LTS) |
| Framework | Spring Boot 3.5.16 |
| Extraction PDF | Apache PDFBox 3.0.3 |
| Rendu graphique | PDFBox — `PDFRenderer` |
| Extraction DOCX | Apache POI — XWPF 5.3.0 |
| OCR | Tess4J 5.13.0 (liaison Tesseract) |
| Similarité textuelle | Apache Commons Text 1.12.0 (Jaro-Winkler) |
| Documentation API | springdoc-openapi 2.8.17 |
| Persistance | aucune — service sans état |
| Tests | JUnit 5, AssertJ, Mockito, JaCoCo |
| Conteneurisation | Docker, Docker Compose |

### Écart assumé par rapport au cahier des charges

Le cahier annonce **Spring Boot 3.2** et **springdoc-openapi 2.6.0**. Ces deux versions ne sont pas
compatibles entre elles — springdoc 2.6 cible Spring Boot 3.3 — et la branche 3.2 ne reçoit plus de
correctifs de sécurité en libre accès. Bâtir un projet de sécurité applicative sur une version en fin de
vie serait contradictoire : la pile est portée sur **Spring Boot 3.5.16** et **springdoc 2.8.17**, dernières
versions supportées de leurs branches respectives. Le reste de la pile est conforme au cahier.

---

## 4. Démarrage

### Avec Docker (recommandé)

```bash
docker compose up --build
```

L'image embarque Tesseract et ses données de langue française et anglaise.

### En local

Le projet cible **Java 21**. Si plusieurs JDK sont installés, désignez explicitement le bon :

```powershell
# Windows / PowerShell
$env:JAVA_HOME = "<chemin vers le JDK 21>"
mvn clean test
mvn spring-boot:run
```

```bash
# Linux / macOS
export JAVA_HOME=/chemin/vers/jdk-21
mvn clean test
mvn spring-boot:run
```

### Points d'entrée

| URL | Description |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Console de démonstration et documentation OpenAPI |
| `http://localhost:8080/v3/api-docs` | Spécification OpenAPI (JSON) |
| `http://localhost:8080/actuator/health` | État du service |

---

## 5. API

| Méthode | Ressource | Description | État |
|---|---|---|---|
| `POST` | `/api/v1/inspect` | Rapport de détection complet | à venir (J5) |
| `POST` | `/api/v1/sanitize` | Contenu assaini prêt pour le modèle | à venir (J8) |
| `POST` | `/api/v1/demo/compare` | Pipeline naïf puis pipeline protégé | à venir (J9) |
| `GET` | `/api/v1/rules` | Catalogue des règles, activation, pondération | **disponible** |
| `PUT` | `/api/v1/rules/{code}` | Modifier l'activation ou le poids d'une règle | **disponible** |

---

## 6. Configuration

Seuils et pondérations sont externalisés dans `src/main/resources/application.yml`, sous le préfixe
`cvguard`. Ils sont également modifiables **à chaud** via `PUT /api/v1/rules/{code}`, sans redémarrage ni
recompilation. Les modifications à chaud ne sont pas persistées : un redémarrage restitue la configuration
déclarée.

```yaml
cvguard:
  detection:
    thresholds:
      min-font-size-pt: 3.0
      min-contrast-ratio: 1.5
      suspect-score: 25
      malicious-score: 60
    rules:
      R7: { enabled: true, weight: 40 }
```

---

## 7. État d'avancement

| Lot | Travaux | État |
|---|---|---|
| J1 | Cadrage, structuration en couches, configuration, moteur d'orchestration, agrégateur de score | **fait** |
| J2 | Extraction PDF avec attributs de rendu (PDFBox) | **fait** |
| J3 | Extraction DOCX (POI), normalisation Unicode, caractères invisibles | **fait** |
| J4 | Règles R1, R2, R3 | à faire |
| J5 | Règles R4, R5 ; endpoint `/inspect` | à faire |
| J6 | Corpus de validation (70 documents) | à faire |
| J7 | Contrôle OCR (Tesseract), règle R6 | à faire |
| J8 | Règle R7, politique d'assainissement | à faire |
| J9 | Démonstrateur comparatif, mesures sur le corpus | à faire |
| J10 | Docker Compose, tests complémentaires, documentation | à faire |

L'image Docker et le fichier `docker-compose.yml` sont rédigés mais leur construction n'a pas encore été
vérifiée : elle le sera au lot J7, lorsque l'intégration de Tesseract sera effectivement exercée.

### Note technique — extraction PDF (J2)

Trois écarts entre PDFBox 3 et ce qu'un usage superficiel de `PDFTextStripper` laisserait supposer,
déterminants pour la fiabilité des règles R1 à R3 :

1. **La couleur n'est pas enregistrée par défaut.** `PDFTextStripper` n'installe que les opérateurs utiles
   à l'extraction de texte ; les opérateurs de couleur (`rg`, `g`, `k`, `sc`, `scn`) en sont absents, si bien
   que l'état graphique reste au noir par défaut quelle que soit la couleur réellement peinte. Un texte
   blanc sur blanc serait restitué comme noir. `RenderAwareTextStripper` enregistre ces opérateurs
   explicitement.
2. **La taille déclarée n'est pas la taille rendue.** `TextPosition.getFontSizeInPt()` retourne la taille
   passée à l'opérateur `Tf`, sans tenir compte de la matrice de transformation courante. Un corps 12
   déclaré puis réduit d'un facteur 10 par la matrice produit un texte rendu à 1,2 pt tout en annonçant 12
   — une règle R1 fondée sur cette méthode serait trivialement contournable. C'est `getYScale()` qui
   restitue la taille effectivement rendue, et c'est elle qui est retenue.
3. **L'état graphique n'est lisible qu'au moment du glyphe.** `PDFTextStripper` collecte tous les glyphes
   d'une page avant d'appeler `writeString` ; à cet instant le flux de contenu est intégralement consommé
   et l'état graphique a repris sa valeur initiale. Couleur et opacité sont donc relevées dans
   `processTextPosition`, seul point où l'état correspond réellement au glyphe peint.

Le recouvrement d'un fragment par un élément graphique opaque (aplat, image) — le troisième vecteur du
chapitre 1 — n'est pas traité à ce stade : il relève de la logique de la règle **R3** elle-même
(« positionnement hors zone [...] ou recouvert par un élément graphique opaque », chapitre 8) et sera
implémenté à son tour au lot J4, avec une analyse de l'ordre de peinture du flux de contenu.

Un quatrième vecteur, absent de l'énumération initiale du chapitre 1, a été identifié en cours
d'implémentation : le mode de rendu de texte invisible (`Tr 3`), légitimement utilisé par la couche OCR
d'un PDF scanné mais détournable pour dissimuler une instruction sans toucher ni à la couleur ni à
l'opacité. `RenderAwareTextStripper` le relève désormais (`TextFragment.hidden`), et `effectiveColor()`
traite un fragment masqué comme un contraste nul — même traitement que le blanc sur blanc, pour la
règle R2.

### Note technique — extraction DOCX et normalisation (J3)

**Périmètre DOCX.** Le cahier des charges limite volontairement l'extraction DOCX à l'accès aux propriétés
de style au niveau du XML (chapitre 7.4 : Apache POI, pas de moteur de mise en page). Trois conséquences en
découlent, assumées et documentées dans `DocxDocumentExtractor` :

1. **Aucune position.** `ExtractedDocument.pages()` reste vide et chaque fragment a une boîte englobante
   nulle : la règle R3 ne s'applique pas à un DOCX.
2. **Aucun fond.** La trame de fond d'un paragraphe (`w:shd`) n'est pas relevée ; le fond retenu reste le
   blanc par défaut. Une injection dissimulée par correspondance de trame plutôt que de couleur de police
   échapperait à R2 dans cette version.
3. **Corps par défaut approché.** Un run sans taille explicite hérite en principe d'une cascade de styles
   (run → paragraphe → styles par défaut du document) ; reconstruire cette cascade est hors périmètre. Un
   corps conventionnel de 11 pt — le défaut usuel de Word et LibreOffice — est retenu à la place.

**Texte masqué natif (`w:vanish`).** Contrairement aux limites ci-dessus, ce vecteur est pleinement pris en
charge : Format > Police > Masqué est le moyen le plus direct de dissimuler une instruction dans un DOCX,
accessible sans aucune manipulation de couleur ou de corps. Comme pour le mode `Tr 3` en PDF, un fragment
masqué est traité comme un contraste nul par la règle R2.

**Point de vigilance POI.** `XWPFDocument.getComments()` lève une `NullPointerException` interne à la
bibliothèque lorsque le document ne déclare aucune partie commentaires — le cas de la grande majorité des
documents. L'appel doit être gardé par `getDocComments() != null`, sans quoi *tout* document DOCX sans
commentaire échouerait à l'extraction. Découvert par test (`handlesEmptyDocument` échouait sur un document
entièrement vide), corrigé dans `DocxDocumentExtractor`.

**Normalisation Unicode (étape [2]).** `TextNormalizer` ramène chaque fragment en forme de compatibilité
NFKC : sans cette étape, un attaquant pourrait rédiger son instruction en caractères pleine chasse
(`ＩＧＮＯＲＥ` → `IGNORE`) pour échapper à une détection lexicale fondée sur une expression régulière
simple (règle R7, à venir). NFKC ne décompose ni ne supprime les caractères de largeur nulle ni les marques
directionnelles : la normalisation peut donc s'appliquer sans effacer les indices que la règle R4 doit
détecter (`InvisibleCharacterScanner`, seize points de code couvrant les deux familles du chapitre 1).

Ces deux normalisations sont appliquées uniformément aux deux formats, dans les extracteurs eux-mêmes plutôt
que dans un décorateur séparé — NFKC étant idempotente, l'appliquer à une valeur déjà normalisée (cas des
métadonnées, lues puis réutilisées pour construire les fragments dédiés) est sans effet.

**Point d'entrée unifié.** `UnifiedDocumentExtractor` dispatche vers l'extracteur du format concerné,
déterminé à partir du nom de fichier déclaré. Prendre en charge un nouveau format se limite à publier une
nouvelle implémentation de `DocumentExtractor` (ENF-07) ; c'est ce composant que l'orchestrateur
d'inspection (J5) appellera.

---

## 8. Portée

Ce projet a pour finalité la **défense** d'une chaîne de traitement documentaire. Les documents piégés du
corpus de validation sont produits en environnement clos, à seule fin de valider les règles de détection,
et ne sont destinés à aucun usage réel. Aucun CV réel de tiers n'est utilisé (contrainte RGPD / loi 09-08).
