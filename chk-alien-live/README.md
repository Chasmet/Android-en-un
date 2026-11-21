# CHK Alien Live — projet Android 0.3.0

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

