param(
    [string]$OutputDirectory = "keys",
    [int]$KeySize = 2048,
    [switch]$Force
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([System.IO.Path]::IsPathRooted($OutputDirectory)) {
    $resolvedOutputDirectory = $OutputDirectory
} else {
    $resolvedOutputDirectory = Join-Path $repoRoot $OutputDirectory
}

New-Item -ItemType Directory -Path $resolvedOutputDirectory -Force | Out-Null

$privateKeyPath = Join-Path $resolvedOutputDirectory "jwt-private.pem"
$publicKeyPath = Join-Path $resolvedOutputDirectory "jwt-public.pem"

if (!$Force -and ((Test-Path -LiteralPath $privateKeyPath) -or (Test-Path -LiteralPath $publicKeyPath))) {
    throw "JWT RSA key files already exist in '$resolvedOutputDirectory'. Use -Force to overwrite them."
}

$temporaryDirectory = Join-Path ([System.IO.Path]::GetTempPath()) ([System.Guid]::NewGuid().ToString())
$javaSourcePath = Join-Path $temporaryDirectory "JwtRsaKeyGenerator.java"

try {
    New-Item -ItemType Directory -Path $temporaryDirectory -Force | Out-Null
    @'
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
'@ | Set-Content -LiteralPath $javaSourcePath -Encoding ASCII

    & java $javaSourcePath $resolvedOutputDirectory $KeySize
    if ($LASTEXITCODE -ne 0) {
        throw "Java key generation failed with exit code $LASTEXITCODE."
    }
} finally {
    Remove-Item -LiteralPath $temporaryDirectory -Recurse -Force -ErrorAction SilentlyContinue
}

Write-Host "Generated JWT RSA key files:"
Write-Host "  Private: $privateKeyPath"
Write-Host "  Public:  $publicKeyPath"
Write-Host ""
Write-Host "Use these .env settings for the default output directory:"
Write-Host "JWT_RSA_PRIVATE_KEY_PATH=file:./keys/jwt-private.pem"
Write-Host "JWT_RSA_PUBLIC_KEY_PATH=file:./keys/jwt-public.pem"
