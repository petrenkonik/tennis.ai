package com.rebuild.tennisai.core.model

/** The type of stroke being analyzed. */
enum class StrokeType(val id: String) {
    SERVE("serve"),
    FOREHAND("forehand"),
    BACKHAND("backhand");

    companion object {
        fun fromId(id: String?) = entries.firstOrNull { it.id == id } ?: SERVE
    }
}

/**
 * Stroke phases M0..M6 (as in the original).
 * Serve: Ready, Stance, Toss&Backswing, Loading/Trophy, Extension, Contact, Recovery.
 * Groundstrokes: Ready, Split Step, Footwork/Unit Turn, Drive/Load, Contact, Follow-Through, Recovery.
 */
enum class Moment(val key: String) {
    M0("M0"), M1("M1"), M2("M2"), M3("M3"), M4("M4"), M5("M5"), M6("M6");

    fun title(stroke: StrokeType, lang: Lang): String {
        val en = when (stroke) {
            StrokeType.SERVE -> when (this) {
                M0 -> "Ready"; M1 -> "Stance"; M2 -> "Toss & Backswing"; M3 -> "Loading"
                M4 -> "Extension"; M5 -> "Contact Point"; M6 -> "Recovery"
            }
            else -> when (this) {
                M0 -> "Ready Position"; M1 -> "Split Step"; M2 -> "Unit Turn"; M3 -> "Drive"
                M4 -> "Contact Point"; M5 -> "Follow-Through"; M6 -> "Recovery"
            }
        }
        val ru = when (stroke) {
            StrokeType.SERVE -> when (this) {
                M0 -> "Готовность"; M1 -> "Стойка"; M2 -> "Подброс и замах"; M3 -> "Загрузка"
                M4 -> "Разгибание"; M5 -> "Точка контакта"; M6 -> "Восстановление"
            }
            else -> when (this) {
                M0 -> "Исходная позиция"; M1 -> "Сплит-шаг"; M2 -> "Замах (unit turn)"; M3 -> "Ускорение"
                M4 -> "Точка контакта"; M5 -> "Проводка"; M6 -> "Восстановление"
            }
        }
        return if (lang == Lang.RU) ru else en
    }

    fun hint(stroke: StrokeType, lang: Lang): String {
        val en = when (stroke) {
            StrokeType.SERVE -> when (this) {
                M0 -> "Reset between points: racket in front, knees soft."
                M1 -> "Feet planted sideways, weight on the back foot."
                M2 -> "Toss arm lifts the ball; hitting arm coils into the trophy."
                M3 -> "Knees load deepest here — the spring compresses."
                M4 -> "Legs, hips, trunk and arm extend up into the ball."
                M5 -> "Full extension; contact above and in front of the head."
                M6 -> "Land balanced inside the court, back foot kicks up."
            }
            else -> when (this) {
                M0 -> "Athletic stance, racket in front, weight on toes."
                M1 -> "Small hop; feet land wider than shoulders."
                M2 -> "Shoulders coil away from the net; racket back."
                M3 -> "Weight loads the back leg, body coils."
                M4 -> "Contact out in front, hips and shoulders opening."
                M5 -> "Racket sweeps up and across; shoulders level."
                M6 -> "Recover to the ready position for the next ball."
            }
        }
        val ru = when (stroke) {
            StrokeType.SERVE -> when (this) {
                M0 -> "Сброс между подачами: ракетка впереди, колени мягкие."
                M1 -> "Ноги развёрнуты боком, вес на дальней ноге."
                M2 -> "Левая рука поднимает мяч; бьющая рука скручивается в «трофи»."
                M3 -> "Колени максимально заряжены здесь — пружина сжата."
                M4 -> "Ноги, бёдра, корпус и рука разгибаются вверх к мячу."
                M5 -> "Полное вытяжение; контакт выше и впереди головы."
                M6 -> "Приземление в равновесии внутри корта, задняя нога уходит назад."
            }
            else -> when (this) {
                M0 -> "Атлетичная стойка, ракетка впереди, вес на носках."
                M1 -> "Небольшой прыжок; ноги приземляются шире плеч."
                M2 -> "Плечи скручиваются от сетки; ракетка уходит назад."
                M3 -> "Вес загружает заднюю ногу, тело скручено."
                M4 -> "Контакт впереди, бёдра и плечи раскрываются."
                M5 -> "Ракетка идёт вверх и через корпус; плечи горизонтальны."
                M6 -> "Вернись в стойку готовности к следующему мячу."
            }
        }
        return if (lang == Lang.RU) ru else en
    }
}

enum class Lang { RU, EN }
