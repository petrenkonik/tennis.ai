# LinkedIn-пост: REA (v9, человеческий голос)

Дата: 2026-10-19. Две версии: **EN (публикуемая)** и RU (зеркало). ~850 символов.
Стиль: живой, от первого лица, без ярлыков-двоеточий, без тройков-перечислений, без хэштегов (при желании добавить 1–2 вручную).
Ссылки в первый комментарий:
1. https://github.com/morluto/rea — скилл REA
2. https://github.com/skylot/jadx — открытый декомпилятор

Карусель к посту: `post_linkedin/collage_1.png`, `post_linkedin/collage_2.png` (1080×1350, подписи на английском).

---

## Версия EN

I've been playing with REA — an open source skill that lets an AI agent do reverse engineering. Wanted to see if it's really as good as they say, so I handed it a commercial APK.

Short version: it worked. The agent checked the Java code first, found just a wrapper, then pulled every string out of the app's native library and read the logic straight from there. It wrote down what it saw and honestly marked what it couldn't figure out.

To be sure the notes were complete, I had it rebuild the app from them. Once in Kotlin, once for the web. Both builds run.

Took way more than one prompt, but way less time than I expected. And the model doing all this was a cheap flash one.

People don't realize what this means yet: reading someone else's app is becoming a weekend project.

Just research, nothing was copied. Links in comments.

---

## Версия RU

Я тут играюсь с REA — опенсорсный скилл, который позволяет ИИ-агенту заниматься реверс-инжинирингом. Захотел проверить, так ли он хорош, как говорят, и отдал ему коммерческий APK.

Коротко: сработало. Агент сначала посмотрел Java-код и нашёл там только обвязку, потом вытащил все строки из нативной библиотеки приложения и прямо из них прочитал его логику. Что увидел — записал, что не смог понять — честно пометил.

Чтобы убедиться, что записи полные, попросил его пересобрать приложение по ним. Один раз на Kotlin, один раз для веба. Обе сборки запускаются.

Ушло сильно больше одного промпта, но сильно меньше времени, чем я ожидал. И модель везде была дешёвая, flash.

Кажется, люди пока не понимают, что это значит: чтение чужого приложения становится задачей на выходные.

Это было исследование, ничего не копировалось. Ссылки в комментариях.
