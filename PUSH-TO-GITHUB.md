# How to push the full package to GitHub

The agent prepared this directory as a clean git repo with the complete project.

Because the agent environment has limited direct git push credentials for https, do this from **your local terminal** (where you are logged into GitHub):

```bash
cd /home/joakim/RFIDManager-Release-2026-06

# Make sure you have the remote (already set in the prepared dir)
git remote -v

# Push (you may need to authenticate via browser or token if prompted)
git push -u origin master --force

# Or if you have gh CLI logged in:
# gh repo set-default JoaBerra/rfid-manager
# git push -u origin master --force
```

After push, the repo will have:
- Full RFIDManager/ Android sources (with all the rich comments anchoring Architecture-Design-Källkod and the consistent IDs)
- llm-wiki/ with Projektrapport.md, log.md etc.
- rfid-setup/
- Good README.md and .gitignore

Alternative (easiest for release):
1. Go to https://github.com/JoaBerra/rfid-manager/releases/new
2. Upload the tarball `~/RFIDManager-Project-2026-06.tar.gz` as an asset.
3. Tag it v2026-06 or similar.

The tarball + this dir is the authoritative "paketerade projektet".
