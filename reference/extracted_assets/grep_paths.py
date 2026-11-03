import re, sys
pat = re.compile(r"\$(sessionId|uid|userId|videoId|analysisId|strokeType|moment|metric)")
lines = [l.rstrip("\n") for l in open("libapp_strings.txt", encoding="utf-8", errors="replace") if pat.search(l)]
uniq = sorted(set(lines))
print(len(uniq), "matches")
for l in uniq[:80]:
    print(l)
