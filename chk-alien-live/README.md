# CHK Alien Live — projet Android 0.4.0

**État : projet source, pas encore APK validé.** Il faut compiler et tester sur le Honor 200 pour prétendre à une application finale opérationnelle. Aucun APK artificiel n'est inclus.

## Ce que la v0.3 fournit réellement

- Java, `minSdk 21`, `compileSdk 34`, `targetSdk 34`, interface verticale et mode plein écran TikTok LIVE mobile (via partage d'écran TikTok « Jeu mobile »).
- Caméra frontale Camera2, détection locale de la tête par `android.media.FaceDetector` et estimation de mouvements autour de la bouche par analyse des pixels. Les mouvements des lèvres sont **approximatifs**, pas une synchronisation phonétique. Le microphone reste utilisable par TikTok.
- Plusieurs personnages de démonstration, dont deux portraits conceptuels plus détaillés, et une bibliothèque permettant d'importer **tes propres portraits** depuis Android. Importations conservées dans le stockage privé de l'application.
- Trois mondes préinstallés et importation d'un décor personnel. Le monde annunaki reçoit une nouvelle illustration plus détaillée.
- Météo de scène calculée localement : nuages texturés mouvants, poussières, particules, brume, pluie (hors espace), vents, éclairs, variation de l'éclairage, objets distants, mode météo automatique 65 s, intensité réglable.
- Qualité de rendu adaptative cible 20 ou 30 FPS (non mesurée sur l'appareil réel). Le temps GPU/CPU sera variable.
- Gestion de mise à jour par GitHub Releases, vérification HTTPS + SHA-256, confirmation obligatoire de l'installation. Données conservées si `applicationId` et clé de signature ne changent pas.

## Limites à ne pas masquer

- **Pas encore de modèle 3D riggé ou d'avatar photoréaliste animé par 468 points.** Un visage 2D reste un portrait 2D ; un simple fichier photo ne devient pas automatiquement un vrai modèle facial 3D.
- Les images de fond sont des illustrations avec des calques atmosphériques 2D animés, pas des simulations volumétriques 3D.
- L'ouverture de la bouche est estimée à partir de contrastes de l'image, peut être imprécise ou peu visible et dépend de l'éclairage. **Le son n'est jamais lu par l'APK : TikTok conserve son microphone.**
- Android ne permet pas à une application ordinaire de se déclarer caméra virtuelle utilisable directement par TikTok. Utilise TikTok LIVE mobile → Jeu mobile/partage d'écran → ouvrir CHK Alien Live → plein écran.
- L'APK n'a pas encore été compilé et ne dispose d'aucun test instrumenté sur Honor 200.

## Construire l'APK de débogage

Prérequis : JDK 17, Android SDK 34 et Gradle 8.7. Le lanceur `gradlew` utilise la commande `gradle` si installée, sinon télécharge Gradle 8.7 ; le JAR officiel de wrapper n'est pas présent dans ce dépôt.

```sh
python3 tools/verify_source.py
javac tools/ParseJavaSources.java && java -cp tools ParseJavaSources
./gradlew assembleDebug
```

Chemin attendu : `app/build/outputs/apk/debug/app-debug.apk` (package `fr.chk.alienlive.debug`). Les deux premières commandes sont des contrôles de structure et de syntaxe, **pas une compilation Android**.

## GitHub Actions et majauto

1. Créer un dépôt GitHub public `Chasmet/CHK-Alien-Live`. Déposer ce projet à sa racine sur `main`. L'action `.github/workflows/android.yml` compile le debug et le publie comme Artifact.
2. Pour que l'action produise une Release stable installable et les mises à jour, configurer **une seule fois** les secrets suivants : `APK_KEYSTORE_B64`, `APK_KEYSTORE_PASSWORD`, `APK_KEY_ALIAS`, `APK_KEY_PASSWORD`. Une même clé doit être réutilisée à chaque version.
3. Chaque `push main` déclenche alors `assembleRelease`, publie l'APK signé, son `update.json` avec SHA-256 et une Release versionnée (`v0.3.<GITHUB_RUN_NUMBER>`).
4. Installer d'abord la **Release** et non le debug. Dans Réglages → Recherche des nouvelles versions, le contrôle retrouve `releases/latest/download/update.json`, compare les versions et propose l'APK. Android demande une autorisation d'installation et une confirmation.
5. Pour préserver les données, **ne pas désinstaller** l'application avant une mise à jour ; garder le package et la clé de signature identiques.

> Le dépôt mentionné doit exister, et les secrets de signature doivent être configurés. Tant que ce n'est pas le cas, majauto ne peut pas réellement télécharger une Release. Il n'existe pas de skill installé nommé exactement `skillmajauto` dans cet environnement ; le mécanisme est reproduit dans `UpdateManager.java` et le workflow.

## Vérifications mobiles nécessaires avant diffusion publique

- Accorder caméra et vérifier que le suivi de tête fonctionne en lumière normale, puis en intérieur sombre.
- Parler pendant TikTok LIVE mobile : contrôler que le micro TikTok reste actif et que la caméra CHK n'est pas réservée par TikTok.
- Tester bouche et mouvements sur plusieurs personnages, avec masque facial et sans masque.
- Contrôler 20/30 FPS, chauffe, batterie, notification et partage d'écran pendant 20 minutes.
- Tester chacun des 6 modes météo sur les 4 scènes (dont la scène importée), les permissions d'import et le retour au mode plein écran.
- Installer une Release signée v1 puis une v2 signée avec la même clé, confirmer la conservation des personnages/décors personnalisés et des réglages.

## Périmètre réseau et confidentialité

Les visuels, l'analyse du visage et les décors fonctionnent localement. Seule la fonction de mise à jour effectue des requêtes HTTPS vers GitHub. La diffusion elle-même est assurée par TikTok, pas par CHK Alien Live.

## Correctif vocal 0.4

Le moteur `VoiceEngine` lit le microphone (avec permission) et produit localement une voix plus grave ou métallique sur la sortie média Android. Il conserve la durée du discours grâce à deux têtes de lecture à fenêtres croisées ; le rendu est expérimental. Choisir le preset en appuyant longuement sur le bouton Voix puis revenir au LIVE et activer la voix.

**Limite critique :** la sortie audio MEDIA n’est pas le microphone TikTok. L’audio ne sera audible par les spectateurs que si le partage d’écran TikTok inclut le son de l’application, que TikTok ne monopolise pas le micro et que le micro TikTok peut être coupé pour éviter une voix doublée. Cela n’a pas été validé sur Honor 200.

Cette version ne fournit toujours pas de personnage facial 3D riggé. Les portraits sont des images réalistes en 2D avec mouvements proxy.
