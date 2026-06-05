#!/usr/bin/env bash
set -euo pipefail

output_directory="keys"
key_size="2048"
force="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --output-directory)
      output_directory="$2"
      shift 2
      ;;
    --key-size)
      key_size="$2"
      shift 2
      ;;
    --force)
      force="true"
      shift
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 1
      ;;
  esac
done

script_directory="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd -- "$script_directory/.." && pwd)"

case "$output_directory" in
  /*) resolved_output_directory="$output_directory" ;;
  *) resolved_output_directory="$repo_root/$output_directory" ;;
esac

private_key_path="$resolved_output_directory/jwt-private.pem"
public_key_path="$resolved_output_directory/jwt-public.pem"

mkdir -p "$resolved_output_directory"

if [[ "$force" != "true" && ( -e "$private_key_path" || -e "$public_key_path" ) ]]; then
  echo "JWT RSA key files already exist in '$resolved_output_directory'. Use --force to overwrite them." >&2
  exit 1
fi

temporary_directory="$(mktemp -d)"
trap 'rm -rf "$temporary_directory"' EXIT

java_source_path="$temporary_directory/JwtRsaKeyGenerator.java"
cat > "$java_source_path" <<'JAVA'
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

public class JwtRsaKeyGenerator {
    public static void main(String[] args) throws Exception {
        Path outputDirectory = Path.of(args[0]);
        int keySize = Integer.parseInt(args[1]);

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(keySize);
        KeyPair keyPair = generator.generateKeyPair();

        Files.createDirectories(outputDirectory);
        writePem(outputDirectory.resolve("jwt-private.pem"), "PRIVATE KEY", keyPair.getPrivate().getEncoded());
        writePem(outputDirectory.resolve("jwt-public.pem"), "PUBLIC KEY", keyPair.getPublic().getEncoded());
    }

    private static void writePem(Path path, String type, byte[] der) throws Exception {
        String lineSeparator = System.lineSeparator();
        String body = Base64.getMimeEncoder(64, lineSeparator.getBytes(StandardCharsets.US_ASCII))
                .encodeToString(der);
        String pem = "-----BEGIN " + type + "-----" + lineSeparator
                + body + lineSeparator
                + "-----END " + type + "-----" + lineSeparator;
        Files.writeString(path, pem, StandardCharsets.US_ASCII);
    }
}
JAVA

java "$java_source_path" "$resolved_output_directory" "$key_size"

echo "Generated JWT RSA key files:"
echo "  Private: $private_key_path"
echo "  Public:  $public_key_path"
echo
echo "Use these .env settings for the default output directory:"
echo "JWT_RSA_PRIVATE_KEY_PATH=file:./keys/jwt-private.pem"
echo "JWT_RSA_PUBLIC_KEY_PATH=file:./keys/jwt-public.pem"
