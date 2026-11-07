# Build GoldApp on your phone — GitHub Actions (no PC)

Same route as Warden. Your phone uploads the project,
GitHub's servers build the APK, you download it.

## 1. Unzip
Unzip `GoldApp-github-ready.zip` on your phone. Keep the folder structure.
You should see `android/`, `README.md`, `GITHUB_BUILD.md`, and a hidden
`.github/` folder (your file manager may not show dot-folders — it is
there, do not worry).

## 2. Create an empty GitHub repo
On github.com, create a new repo, e.g. `goldapp`.
Do NOT tick "Add a README", ".gitignore", or a licence — an empty repo
avoids a conflict on the first push.

## 3. Push from Termux (as for Warden)
In Termux, with `git` and `gh` installed and `gh auth login` done:
```
cd ~/goldapp        # the extracted folder
git init
git add .
git commit -m "GoldApp v0.1"
git branch -M main
git remote add origin https://github.com/<your-username>/goldapp.git
git push -u origin main
```

## 4. Wait for the build
On GitHub, open your repo > Actions > "Build Android APK".
Wait for the green check (about 2-3 minutes).

## 5. Download and install
In the finished run, download the `goldapp-debug-apk` artifact, unzip
it, and install `app-debug.apk` on your phone.

## 6. Open the app
No key, no account, no setup. Open GoldApp, tap Refresh, and the live
gold price should load. If it says it couldn't load, check your
connection and tap Refresh again — the last saved price stays visible.
