package ma.emsi.cvguard.domain.model;

/**
 * Provenance d'un fragment de texte au sein du document.
 *
 * <p>La distinction est necessaire car les regles ne s'appliquent pas uniformement :
 * un fragment de metadonnee n'a ni police ni position, et seule la regle R5 le
 * concerne.</p>
 */
public enum FragmentOrigin {

    /** Corps du document, rendu sur une page. */
    BODY,

    /** En-tete de page. */
    HEADER,

    /** Pied de page. */
    FOOTER,

    /** Champ de metadonnee (Title, Subject, Keywords, Author...). */
    METADATA,

    /** Commentaire ou revision (annotations PDF, commentaires DOCX). */
    COMMENT,

    /** Texte alternatif d'une image ou d'une forme. */
    ALT_TEXT
}
