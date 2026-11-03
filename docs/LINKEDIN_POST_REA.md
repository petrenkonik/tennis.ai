# LinkedIn post: REA (v9, human voice)

Date: 2026-10-19. Two versions: **EN (the one to publish)** and RU (mirror). ~850 characters.
Style: lively, first person, no colon-labels, no lists of three, no hashtags (optionally add 1–2 manually).
Links in the first comment:
1. https://github.com/morluto/rea — the REA skill
2. https://github.com/skylot/jadx — open-source decompiler

Carousel for the post: `post_linkedin/collage_1.png`, `post_linkedin/collage_2.png` (1080×1350, captions in English).

---

## EN version

I've been playing with REA — an open source skill that lets an AI agent do reverse engineering. Wanted to see if it's really as good as they say, so I handed it a commercial APK.

Short version: it worked. The agent checked the Java code first, found just a wrapper, then pulled every string out of the app's native library and read the logic straight from there. It wrote down what it saw and honestly marked what it couldn't figure out.

To be sure the notes were complete, I had it rebuild the app from them. Once in Kotlin, once for the web. Both builds run.

Took way more than one prompt, but way less time than I expected. And the model doing all this was a cheap flash one.

People don't realize what this means yet: reading someone else's app is becoming a weekend project.

Just research, nothing was copied. Links in comments.

---

## RU version

I've been playing with REA — an open source skill that lets an AI agent do reverse engineering. I wanted to check whether it's really as good as they say, so I handed it a commercial APK.

In short: it worked. The agent first looked at the Java code and found only a wrapper there, then pulled all the strings out of the app's native library and read its logic straight from them. What it saw, it wrote down; what it couldn't understand, it honestly marked.

To make sure the notes were complete, I asked it to rebuild the app from them. Once in Kotlin, once for the web. Both builds run.

It took far more than one prompt, but far less time than I expected. And the model was a cheap flash one throughout.

It seems people don't yet understand what this means: reading someone else's app is becoming a weekend task.

This was research, nothing was copied. Links in the comments.
