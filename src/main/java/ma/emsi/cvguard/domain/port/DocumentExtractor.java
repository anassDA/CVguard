package ma.emsi.cvguard.domain.port;

import ma.emsi.cvguard.domain.model.DocumentFormat;
import ma.emsi.cvguard.domain.model.ExtractedDocument;

/**
 * Port d'extraction documentaire (etape [1] de la chaine de traitement).
 *
 * <p>Le domaine ignore par quelle bibliotheque le document est ouvert. Cette frontiere
 * est ce qui permet de tester l'integralite du moteur de regles a partir de documents
 * construits en memoire, sans fichier PDF ni dependance native.</p>
 */
public interface DocumentExtractor {

    /** Format pris en charge par cet extracteur. */
    DocumentFormat format();

    /**
     * Extrait le contenu et les attributs de rendu du document.
     *
     * @param content    contenu binaire du document, traite en memoire (ENF-05)
     * @param documentId identifiant de correlation de l'inspection
     * @param filename   nom de fichier declare, a des fins de journalisation
     * @throws DocumentExtractionException si le document est illisible ou corrompu
     */
    ExtractedDocument extract(byte[] content, String documentId, String filename);
}
