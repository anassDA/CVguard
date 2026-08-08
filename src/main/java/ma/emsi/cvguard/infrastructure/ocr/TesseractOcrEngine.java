package ma.emsi.cvguard.infrastructure.ocr;

import java.awt.image.BufferedImage;
import ma.emsi.cvguard.config.CvGuardProperties;
import ma.emsi.cvguard.domain.port.OcrEngine;
import ma.emsi.cvguard.domain.port.OcrException;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Moteur OCR adosse a Tesseract via Tess4J (etape [4], regle R6).
 *
 * <p>L'integration de Tesseract est la plus incertaine du projet : bibliotheque native
 * et donnees de langue doivent etre presentes sur l'hote. La disponibilite est sondee
 * une seule fois — une reconnaissance sur une image vide est trop leger pour verifier
 * les donnees de langue, il est donc procede a une veritable reconnaissance minimale —
 * puis memorisee. Lorsque le moteur est indisponible, {@link #isAvailable()} retourne
 * {@code false} et la regle R6 se retire au lieu d'echouer (analyse de risque du
 * cahier des charges) : le service reste fonctionnel et demontrable sans lui.</p>
 *
 * <p>L'instance de Tesseract n'est pas thread-safe : les reconnaissances sont
 * serialisees, ce qui borne le cout CPU d'un service sans etat.</p>
 */
@Component
public class TesseractOcrEngine implements OcrEngine {

    private static final Logger log = LoggerFactory.getLogger(TesseractOcrEngine.class);

    private final Tesseract tesseract = new Tesseract();

    private final String language;

    private volatile boolean availabilityKnown;
    private volatile boolean available;

    public TesseractOcrEngine(CvGuardProperties properties) {
        CvGuardProperties.Ocr config = properties.getOcr();
        this.language = config.getLanguage();
        tesseract.setLanguage(language);
        if (config.getDataPath() != null && !config.getDataPath().isBlank()) {
            tesseract.setDatapath(config.getDataPath());
        }
    }

    @Override
    public boolean isAvailable() {
        if (!availabilityKnown) {
            probe();
        }
        return available;
    }

    @Override
    public synchronized String recognize(BufferedImage page) {
        if (!isAvailable()) {
            throw new OcrException("Le moteur OCR est indisponible sur cet hote");
        }
        try {
            return tesseract.doOCR(page);
        } catch (TesseractException failure) {
            throw new OcrException("La reconnaissance optique a echoue : " + failure.getMessage(), failure);
        }
    }

    /**
     * Sonde la presence de la bibliotheque native et des donnees de langue par une
     * reconnaissance minimale, puis memorise le resultat pour les appels suivants.
     */
    private synchronized void probe() {
        if (availabilityKnown) {
            return;
        }
        try {
            tesseract.doOCR(probeImage());
            available = true;
            log.info("Moteur OCR operationnel (langues : {})", language);
        } catch (Throwable failure) {
            available = false;
            log.warn("Moteur OCR indisponible sur cet hote, la regle R6 se retire : {}",
                    failure.getClass().getSimpleName()
                            + (failure.getMessage() == null ? "" : " : " + failure.getMessage()));
        }
        availabilityKnown = true;
    }

    /**
     * Image de sonde : un caractere sur fond blanc, assez grande pour que Tesseract
     * l'accepte comme une page vide plutot que comme une entree invalide.
     */
    private static BufferedImage probeImage() {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_BYTE_GRAY);
        var graphics = image.createGraphics();
        graphics.setColor(java.awt.Color.WHITE);
        graphics.fillRect(0, 0, 64, 64);
        graphics.setColor(java.awt.Color.BLACK);
        graphics.drawString("A", 24, 40);
        graphics.dispose();
        return image;
    }
}
