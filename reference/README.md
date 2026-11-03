# reference/ — реверс оригинала (только чтение)

Материалы реверс-инжиниринга `com.tennis.ai` v2.0.19. Эта директория **не билдится**
и не входит в поставку; здесь — источник знаний для портирования.

## Содержимое

| Путь | Что это |
|---|---|
| `ANALYSIS_FINDINGS.md` | Главный отчёт: стек, бэкенд, пайплайн анализа, фазы M0–M6, пороги, поправки, drills, Axel, квоты |
| `DECISIONS.md` | Зафиксированные решения по ребилду (стек TMA + Nest.js + Mongo + Stars, структура, видео-пайплайн) |
| `apks/com.tennis.ai_v2.0.19.apks.apk` | Оригинальный App Bundle (243 МБ) |
| `apks/apks_extracted/` | Распакованный бандл: base.apk + split_config.arm64_v8a.apk |
| `extracted_assets/` | Артефакты анализа Dart AOT: `libapp_strings.txt` (29 620 строк — основной источник), `long_strings.txt`, `all_identifiers.txt`, переводы, модели YOLO, `grep_paths.py` |
| `rea-outputs/` | Сырые JSON-выдачи REA (`pkg_evidence.json`, классификации) |

## Ключевые идентификаторы

- REA Evidence: `ev_7f8b10ab273ceac2442806b5363b4ca749f9d4b8539ad360c34b927ccf5dd516`
- base.apk SHA-256: `bb423559aaf67256638694641d81a279c3e386b6bf805b0cd6de2338bc1478e3`
- Инструменты: REA CLI 6.5.0 + jadx-headless-mcp 0.7.1 (JAR SHA-256 `6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75`)

## Что портировать дальше

Список Dart-сервисов → целевые модули см. [../../docs/PORTING.md](../../docs/PORTING.md).
