# CVGuard — Note de synthèse : positionnement OWASP LLM Top 10

Note rédigée à la fin du projet (livrable L6 du cahier des charges). Référentiel :
OWASP Top 10 for LLM Applications, édition 2025.

## Positionnement

**CVGuard contre LLM01 — Prompt Injection.** La menace première traitée par le projet
est l'injection de prompt : un candidat dissimule dans son CV une instruction
adressée au modèle d'évaluation. Le système répond par la **défense en profondeur**
sur les trois couches de la chaîne :

| Couche | Mesure | OWASP |
|---|---|---|
| Détection | 7 règles (R1-R7) sur les attributs de rendu (taille, contraste, position), les caractères invisibles, les métadonnées, l'écart OCR et la structure d'instruction | LLM01 : détecter |
| Neutralisation | Purge des fragments incriminés, normalisation NFKC, retrait des caractères de contrôle, encapsulation dans des délimiteurs explicites | LLM01 : isoler les données |
| Atténuation | Palette SUSPECT imposant une revue humaine ; pondération conservatrice ; score logarithmique évitant la condamnation par répétition d'un signal faible | LLM01 : contenir l'impact |

## Traitement des autres entrées du Top 10

- **LLM02 — Insecure Output Handling** : le pipeline protégé ne transmet au modèle
  que du contenu encapsulé ; la réponse du modèle n'est jamais réinjectée dans le
  document. Hors périmètre côté serveur.
- **LLM03 — Prompt Leakage** : la consigne système envoyée au modèle est
  externalisée en configuration, sans données confidentielles de l'entreprise.
- **LLM06 — Excessive Agency** : le service n'a ni persistance, ni accès réseau
  sortant en mode simulé (défaut) ; l'appel HTTP au modèle est optionnel et borné
  (timeout 30 s).
- **LLM10 — Unbounded Consumption** : contrôle OCR conditionnel (ENF-02), rendu
  limité à 5 pages, taille des extraits bornée, documents traités en mémoire puis
  supprimés (ENF-05).
- **LLM08 — Vector and Embedding Weaknesses** : sans objet — le service est sans
  état, aucun corpus vectoriel n'est construit.

## Risques résiduels assumés

1. **R7 est lexicale et déterministe** : une formulation hors motifs couverts
   échappe à la détection. La défense repose alors sur les couches structurelles
   (R1-R6) et l'isolation (encapsulation), comme le prévoit le cahier des charges.
2. **Faux négatifs OCR** : un texte dissimulé par correspondance de trame de fond
   (`w:shd`) échappe à R2 pour un DOCX (aucun moteur de mise en page, chapitre 7.4).
3. **Faux positifs** : la pondération du cahier et le palier SUSPECT — revue humaine
   obligatoire — garantissent qu'aucune candidature n'est écartée par la seule
   décision du système.

## Validation

Corpus de 70 documents : taux de détection **100 %** (exigence ≥ 90 %), taux de
faux positifs **0 %** (exigence < 10 %) — détail dans le README, section 7.
