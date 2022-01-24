import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.clear
import kotlinx.html.div
import kotlinx.html.dom.append
import kotlinx.html.id
import kotlinx.html.js.onClickFunction
import org.w3c.dom.HTMLFormElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.Node
import org.w3c.dom.get

data class Letter(
    val letter: Char,
    var status: TileStatus
) {
    fun next(): TileStatus {
        this.status = when (this.status) {
            TileStatus.CORRECT -> TileStatus.PRESENT
            TileStatus.PRESENT -> TileStatus.ABSENT
            TileStatus.ABSENT -> TileStatus.CORRECT
        }

        return this.status
    }
}

data class Row(
    val word: List<Letter>
) {
    companion object {
        fun fromInput(input: String): Row {
            return Row(input.map { Letter(it, TileStatus.ABSENT) })
        }
    }
}

enum class TileStatus {
    CORRECT,
    PRESENT,
    ABSENT
}

var rows: MutableList<Row> = mutableListOf()
var remaining: List<String> = emptyList()

fun main() {
    remaining = words

    window.onload = {
        updateGuessPlaceholder()
        document.body?.renderBoard()
    }

    val form = document.getElementById("guess_form")!! as HTMLFormElement
    val guess = document.getElementById("guess")!! as HTMLInputElement

    form.addEventListener("submit", {
        submit(guess.value)
        guess.value = ""
        it.preventDefault()
    }, false)
}

fun submit(guess: String) {
    if (guess.length != 5) {
        return
    }

    for (row in rows) {
        // Don't allow repeat words
        val word = row.word.map { it.letter }.joinToString("")
        if (word == guess) {
            return
        }
    }

    rows.add(Row.fromInput(guess))

    document.body?.renderBoard()
    updateSummary()
    updateGuessPlaceholder()
}

fun clickTile(rowIdx: Int, letterIdx: Int) {
    val letter = rows[rowIdx].word[letterIdx]
    val tileElement = document.getElementById("${rowIdx}_${letterIdx}")!!

    tileElement.attributes.get("data-state")?.value = letter.next().name

    updateSummary()
    updateGuessPlaceholder()
}

fun updateGuessPlaceholder() {
    if (remaining.count() == 0) {
        console.log("no more remaining")
        return
    }

    val guess = document.getElementById("guess")!! as HTMLInputElement

    val remainingNoDupes = remaining.filter {
        it.toSet().count() == 5
    }

    val bestWordSet = if (remainingNoDupes.count() == 0) {
        remaining
    } else {
        remainingNoDupes
    }

    val bestWord = bestWordSet.map {
        // score all the remaining words
        Pair(it, it.sumOf {
            scores[it]!!
        })
    }.sortedBy { it.second }.first().first

    guess.placeholder = bestWord.uppercase()
}

fun updateSummary() {
    val summaryCount = document.getElementById("summary-count")!!
    remaining = words

    rows.forEach { row ->
        row.word.forEachIndexed { letterIdx, letter ->
            val ch = letter.letter.lowercaseChar()
            when (letter.status) {
                TileStatus.CORRECT -> {
                    remaining = remaining.filter {
                        it[letterIdx] == ch
                    }
                }
                TileStatus.PRESENT -> {
                    remaining = remaining.filter {
                        // remove words that only have one copy of
                        // the letter and its in the wrong spot
                        if (it[letterIdx] == ch) {
                            return@filter it.count { it == ch } != 1
                        }

                        it.contains(ch)
                    }
                }
                TileStatus.ABSENT -> {
                    remaining = remaining.filter {
                        !it.contains(ch)
                    }
                }
                else -> {}
            }
        }
    }

    summaryCount.innerHTML = "${remaining.count()} possible words"

    val summary = document.getElementById("summary-options")!!
    summary.clear()
    if (remaining.count() < 40) {

        summary.append {
            remaining.forEach {
                div(classes = "word") {
                    +"$it"
                }
            }
        }
    }
}

fun Node.renderBoard() {
    val board = document.getElementById("board")!!
    board.clear()

    rows.forEachIndexed { rowIdx, row ->
        board.append {
            div(classes = "row") {
                row.word.forEachIndexed { letterIdx, it ->
                    div(classes = "tile") {
                        id = "${rowIdx}_${letterIdx}"
                        attributes["data-state"] = it.status.name
                        onClickFunction = { clickTile(rowIdx, letterIdx) }
                        +"${it.letter}"
                    }
                }
            }
        }
    }
}