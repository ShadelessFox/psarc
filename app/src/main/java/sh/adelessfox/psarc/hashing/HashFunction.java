package sh.adelessfox.psarc.hashing;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public abstract sealed class HashFunction
    permits Md5Function {

    public static HashFunction md5() {
        return Md5Function.MD5;
    }

    public abstract HashCode hash(byte[] input, int off, int len);

    public HashCode hash(byte[] input) {
        return hash(input, 0, input.length);
    }

    public HashCode hash(CharSequence input, Charset charset) {
        return hash(input.toString().getBytes(charset));
    }

    public HashCode hash(CharSequence input) {
        return hash(input, StandardCharsets.UTF_8);
    }
}
