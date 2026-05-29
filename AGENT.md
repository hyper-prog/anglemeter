# AGENT Instructions

Owner: Project maintainers

## Goal
Build and maintain an Android angle-meter application in Kotlin using Android Studio Panda 4 (2025.3.4), running in full-screen graphical mode with a fixed center cross, a sensor-driven rotating center cross, complementary angle display, and optional live camera preview overlay.

## Instructions
1. Use Kotlin and Android Studio project conventions.
2. Keep the app single-purpose: angle metering with real-time visual feedback.
3. UI framework is Jetpack Compose.
4. Support two overlay color schemes with a top-right toggle icon: black+brown and white+light pink.
5. Dynamic cross must rotate from fused orientation data (game rotation vector primary, rotation vector fallback).
6. Dynamic cross angle must be tilt-compensated (derived from projected world-up on screen plane), not raw roll-only orientation.
7. Cross-angle filtering and threshold checks must be 90-degree periodic to keep landscape behavior stable and avoid wrap-around jumps near 90 degrees.
8. Update angle rendering only when movement threshold is exceeded.
9. Coincidence threshold logic:
- If crosses coincide within threshold, hide angle labels.
- Otherwise show two complementary angles where their sum is always 90.0 degrees.
10. Camera behavior:
- Camera starts disabled by default.
- Camera toggle icon enables or disables live preview.
- Camera switch icon is visible only when camera is enabled.
- Front camera preview is mirrored.
- Front mirror must be applied as vertical-axis mirroring independent of device upside-down rotation.
- Camera-related icons should follow the same orientation readability rotation as angle labels.
11. Coincidence highlight behavior:
- When the two leveling crosses coincide (within threshold), draw the center cross as red with a black outline and thicker stroke.
12. Angle label readability behavior:
- Angle labels must stay left-to-right readable.
- When device tilt exceeds 45 degrees toward landscape, labels must reorient to landscape (±90 degrees) instead of vertical bottom-to-top readability.
13. Angle label layout behavior:
- Do not show both angles in one label; render separate labels in the top-left screen quarter near the two center-adjacent quarter corners (top-right and bottom-left), each indicating the nearby angle.
14. Screen-tilt warning bar behavior:
- Render a translucent red warning bar near the bottom with a small bottom margin, centered and symmetric around the vertical axis.
- Bar width must be proportional to screen tilt away from vertical: hidden when vertical, near wall-to-wall when horizontal.
- Place the bar on the opposite side of the screen from the top-right camera/color controls.
15. Screenshot capture behavior:
- Provide a screenshot/photo button in the bottom-right corner above the red tilt-warning bar.
- Save captures to the device gallery (Pictures/AngleMeter).
- On capture, provide feedback with a brief full-screen flash and standard shutter sound instead of toast messages.
- When camera preview is enabled, the captured screenshot must include the camera image.
- Use window-level screenshot capture (PixelCopy) so Surface-based camera preview is included.
- Screenshot button icon should follow the same orientation readability rotation as labels/camera icons.
16. Cleanup workflow:
- Use root `clean.bat` to remove generated/build artifacts (`.gradle`, module `build` folders, native build caches, and local build logs).
- After `clean.bat`, `build.bat` must still be able to perform a successful build.
17. CI/CD workflow:
- GitHub Actions must build the release APK on GitHub-hosted runners.
- The built release APK must be uploaded as a downloadable workflow artifact.
- On `release: published`, attach the APK to GitHub Release assets as well.
18. Licensing:
- The project is licensed under Apache License 2.0 and must include a root `LICENSE` file with Apache-2.0 terms.
19. App UI text language for first version is English.
20. Minimum Android API is 29.
21. Use root `build.bat` for local CLI builds on this PC; it auto-selects Android Studio JBR and local SDK.
22. `build.bat` without arguments must build release by default.
23. Local release builds are signed with debug signing config for installability during device testing.

## Scope
In scope:
- Sensor pipeline, graphical overlay, camera preview integration, and lifecycle-safe operation.
- Build files and project structure needed for Android Studio opening and sync.

Out of scope:
- Cloud sync, user accounts, analytics backends, and AR overlays.
- Nonessential multi-screen navigation.

## Update Policy
Update this document when any of the following changes:
1. Core goal or target platform/toolchain.
2. Mandatory architecture rules or UI behavior.
3. Sensor algorithm thresholds or camera interaction rules.
4. Language or localization policy.
