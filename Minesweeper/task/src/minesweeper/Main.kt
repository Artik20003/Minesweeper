package minesweeper

import kotlin.random.Random


enum class CellType { EMPTY, DIGIT, MINE}


class Cell(val type: CellType, value: String = "") {

    var isMarked: Boolean = false
    var isOpened: Boolean = false
        set (value) {
            if(!value) throw IllegalArgumentException ("You can open the cell only once")
            if(isEmpty()) this.value = Symbols.EXPLORED
            field = value
            isMarked = false

        }
    object Symbols {
        const val MINE: String = "X"
        const val UNEXPLORED: String = "."
        const val MARKED: String = "*"
        const val EXPLORED: String = "/"
    }
    var value: String = value
        set(value) {
            field = when (type) {
                CellType.DIGIT -> {
                    if (value.toInt() !in 1..8) {
                        throw IllegalArgumentException("The value must be from 1 to 8")
                    }
                    value
                }
                CellType.EMPTY -> {
                    if (value.isEmpty()) {
                        Symbols.UNEXPLORED
                    } else {
                        if(value !in mutableListOf(Symbols.UNEXPLORED, Symbols.EXPLORED)) {
                            throw IllegalArgumentException("Empty cell value should be ${Symbols.UNEXPLORED} or ${Symbols.EXPLORED}")
                        }
                        value
                    }

                }
                CellType.MINE -> Symbols.MINE
            }
        }

    init {
        // этот костыль чтобы  setter сработал, как я понял при конструкторе он не срабатывает, как улучшить?
        this.value = value
    }

    fun toggleMark(): Boolean {
        if(isOpened) throw IllegalArgumentException("Opened cell can't be marked")

        isMarked = !isMarked
        return  isMarked

    }
    fun isMine(): Boolean = type == CellType.MINE
    fun isDigit(): Boolean = type == CellType.DIGIT
    fun isEmpty(): Boolean = type == CellType.EMPTY
}


class Minesweeper (mineCount: Int ) {
    companion object {
        val fieldSize: Int = 9
    }

    enum class GameStatus {WIN, FAILED, IN_PROCESS}
    var gameStatus: GameStatus = GameStatus.IN_PROCESS


    private var field = mutableListOf<MutableList<Cell>>()

    init {
        setMines(mineCount)
    }

    private fun setMines(mineCount: Int = 10) {
        field = MutableList(fieldSize){ MutableList(fieldSize){ Cell(CellType.EMPTY) }}
        var counter = 0
        while (counter < mineCount) {
            val i = Random.nextInt(0, fieldSize)
            val j = Random.nextInt(0, fieldSize)
            if(field[i][j].isEmpty()) {
                field[i][j] = Cell(CellType.MINE)
                counter++
            }
        }
        setDigits()
    }

    // можно приватным делать метод, но я еще не дошел до этой темы
    private fun setDigits() {
        for (i in field.indices) {
            for (j in field[i].indices) {
                if (field[i][j].type != CellType.MINE && getNearMineCount(i, j) > 0) {
                    field[i][j] = Cell(CellType.DIGIT, getNearMineCount(i, j).toString())
                }
            }
        }
    }


    fun printField() {
        // columns header
        println(" │" + (1..fieldSize).joinToString("") + "│")
        println("—│" + "—".repeat(fieldSize)+ "│")
        for (i in field.indices) {
            // row header
            print("${i+1}│")
            for (j in field[i].indices) {
                val cell = field[i][j]
                print(
                    if(gameStatus == GameStatus.FAILED) {
                        cell.value
                    } else {
                        when {
                            cell.isOpened -> cell.value
                            cell.isMarked -> Cell.Symbols.MARKED
                            else -> Cell.Symbols.UNEXPLORED
                        }
                    }
                )
            }
            println("│")
        }
        // закрывающая строка таблицы
        println("—│" + "—".repeat(fieldSize)+ "│")
    }

    fun markMine (x: Int, y:Int) {
        if(x !in 1..fieldSize || y  !in 1..fieldSize)
            throw IllegalArgumentException("Coordinate value Should be  from 1 to $fieldSize")
        if(field[y-1][x-1].isOpened)
            throw IllegalArgumentException("Can't mark Explored  field")

        field[y-1][x-1].toggleMark()
        updateGameOverStatus()
    }

     fun freeCell (x: Int, y: Int) {
         if(x !in 1..fieldSize || y  !in 1..fieldSize)
             throw IllegalArgumentException("Coordinate value Should be  from 1 to $fieldSize")
         if(field[y-1][x-1].isOpened)
             throw IllegalArgumentException("This field is already opened")

         when (field[y-1][x-1].type) {

             CellType.DIGIT, CellType.MINE -> field[y-1][x-1].isOpened = true
             CellType.EMPTY -> recursiveFreeCell(x-1, y-1)
         }
         updateGameOverStatus()

     }

    private fun getNearMineCount(x: Int, y: Int): Int {
        var mineCount = 0
        for (i in -1..1) {
            for (j in -1..1){
                val checkingX = x + i
                val checkingY = y + j
                if(checkingX !in 0 until fieldSize || checkingY !in 0 until fieldSize  || i==0 && j == 0) continue
                if(field[checkingX][checkingY].isMine()) mineCount++
            }
        }
        return mineCount
    }

    private fun recursiveFreeCell(x: Int, y: Int) {
        val cell = field[y][x]

        cell.isOpened = true
        if(cell.isDigit()) return

        for (i in -1..1) {
            for (j in -1..1){
                val checkingX = x + i
                val checkingY = y + j

                if(
                    checkingX !in 0 until fieldSize
                    || checkingY !in 0 until fieldSize
                    || i==0 && j == 0
                    || field[checkingY][checkingX].isOpened
                ) {
                    continue
                }
                recursiveFreeCell (checkingX, checkingY)
            }
        }

    }

    private fun updateGameOverStatus(){
        var allMarkedCorrectly = true
        var fieldHaveEmptyCells = false
        var mineIsOpened = false

        for (i in field.indices) {
            for (j in field[i].indices) {
                val cell = field[i][j]
                //  check if all mines are correctly marked
                if(
                    cell.isMine() && !cell.isMarked ||
                    !cell.isMine() && cell.isMarked
                ) {
                    allMarkedCorrectly = false
                }

                // check empty unopened cells left
                if(!cell.isMine() && !cell.isOpened) fieldHaveEmptyCells = true
                // check mine is not opened
                if(cell.isMine() && cell.isOpened) mineIsOpened = true
            }
        }
        if(mineIsOpened) {
            gameStatus = GameStatus.FAILED
        } else if (allMarkedCorrectly || !fieldHaveEmptyCells){
            gameStatus = GameStatus.WIN
        }
    }
}

fun main() {
    //  ввод количества мин
    var mineCount: Int
    do {
        print("How many mines do you want on the field?")
        try{
            mineCount = readln().trim().toInt()
            if(mineCount !in 1..(Minesweeper.fieldSize * Minesweeper.fieldSize))
                throw IllegalArgumentException()
        } catch (e: java.lang.RuntimeException) {
            println("Mine count should be from 1 to ${Minesweeper.fieldSize * Minesweeper.fieldSize}")
            continue
        }
        break
    } while(true)

    //  создадим объект и выведем поле
    val minesweeper = Minesweeper(mineCount)
    minesweeper.printField()


    // предложим и обработаем ввод очередной команды
    do {
        print("Set/unset mines marks or claim a cell as free:")
        val userInput = readln().trim().split(" ")
        try {
            val x = userInput[0].toInt()
            val y = userInput[1].toInt()
            val cmd = userInput[2]
            when (cmd) {
                "mine" -> minesweeper.markMine(x, y)
                "free" -> minesweeper.freeCell(x,y)
                else -> throw IllegalArgumentException("Wrong input: correct is: x y free|mine")
            }

        } catch( e: IllegalArgumentException) {
            println(e.message)
            continue
        }
        catch (e: java.lang.Exception) {
            println("Wrong input")
            continue
        }
        minesweeper.printField()

    } while (minesweeper.gameStatus == Minesweeper.GameStatus.IN_PROCESS)
    if(minesweeper.gameStatus == Minesweeper.GameStatus.FAILED){
        println("You stepped on a mine and failed!")
    } else {
        println("Congratulations! You found all the mines!")
    }
}
