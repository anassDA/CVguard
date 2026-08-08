package ma.emsi.cvguard.application;

/**
 * Identifiants de correlation (EF-14) : huit octets aleatoires rendus en hexadecimal,
 * suffisamment courts pour les journaux et suffisamment entropiques pour ne jamais
 * se confondre entre deux inspections.
 */
public final class CorrelationIds {

    private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private CorrelationIds() {
    }

    public static String newId() {
        byte[] bytes = new byte[8];
        RANDOM.nextBytes(bytes);
        StringBuilder id = new StringBuilder(16);
        for (byte value : bytes) {
            id.append(HEX[(value >> 4) & 0xF]).append(HEX[value & 0xF]);
        }
        return id.toString();
    }
}
