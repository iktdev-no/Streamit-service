package no.iktdev.streamit.service.db.tables.util

import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table

import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import java.sql.SQLIntegrityConstraintViolationException

open class TableDefaultOperations<T : Table> {

}

data class TransactionResult<T>(
    val result: T,
    val exception: Exception? = null
)

fun <T> withDirtyRead(db: Database? = null, onError: ((Exception) -> Unit)? = null, run: () -> T): T? {
    return try {
        transaction(db = db, transactionIsolation = Connection.TRANSACTION_READ_UNCOMMITTED) {
            try {
                run()
            } catch (e: Exception) {
                // log the error here or handle the exception as needed
                throw e // Optionally, you can rethrow the exception if needed
            }
        }
    } catch (e: Exception) {
        if (onError == null) {
            e.printStackTrace()
        } else {
            onError.invoke(e)
        }
        null
    }
}


fun <T> withTransaction(db: Database? = null, onError: ((Exception) -> Unit)? = null, run: () -> T): T? {
    return try {
        transaction(db) {
            try {
                run()
            } catch (e: Exception) {
                // log the error here or handle the exception as needed
                throw e // Optionally, you can rethrow the exception if needed
            }
        }
    } catch (e: Exception) {
        if (onError == null) {
            e.printStackTrace()
        } else {
            onError.invoke(e)
        }
        null
    }
}

fun <T> withTransaction(
    rollbackOnFailure: Boolean = false,
    run: () -> T
): Result<T> {
    return try {
        val result = transaction {
            try {
                run()
            } catch (e: Exception) {
                if (rollbackOnFailure) rollback()
                throw e
            }
        }
        Result.success(result)
    } catch (e: Exception) {
        e.printStackTrace()
        Result.failure(e)
    }
}



fun <T> insertWithSuccess(db: Database? = null, onError: ((Exception) -> Unit)? = null, run: () -> T): Boolean {
    return try {
        transaction(db) {
            try {
                run()
                commit()
            } catch (e: Exception) {
                // log the error here or handle the exception as needed
                throw e // Optionally, you can rethrow the exception if needed
            }
        }
        true
    } catch (e: Exception) {
        if (onError == null) {
            e.printStackTrace()
        } else {
            onError.invoke(e)
        }
        false
    }
}

fun <T> executeOrException(db: Database? = null, rollbackOnFailure: Boolean = false, run: () -> T): Exception? {
    return try {
        transaction(db) {
            try {
                run()
                commit()
                null
            } catch (e: Exception) {
                // log the error here or handle the exception as needed
                if (rollbackOnFailure)
                    rollback()
                e

            }
        }
    } catch (e: Exception) {
        return e
    }
}

fun <T> executeWithResult(db: Database? = null, onError: ((Exception) -> Unit)? = null, run: () -> T): T? {
    return try {
        transaction(db) {
            try {
                val res = run()
                commit()
                res
            } catch (e: Exception) {
                // log the error here or handle the exception as needed
                rollback()
                throw e
            }
        }
    } catch (e: Exception) {
        if (onError == null) {
            e.printStackTrace()
        } else {
            onError.invoke(e)
        }
        return null
    }
}

fun <T> executeWithStatus(db: Database? = null, onError: ((Exception) -> Unit)? = null, run: () -> T): Boolean {
    return try {
        transaction(db) {
            try {
                run()
                commit()
            } catch (e: Exception) {
                // log the error here or handle the exception as needed
                throw e // Optionally, you can rethrow the exception if needed
            }
        }
        true
    } catch (e: Exception) {
        if (onError == null) {
            e.printStackTrace()
        } else {
            onError.invoke(e)
        }
        false
    }
}

fun Exception.isExposedSqlException(): Boolean {
    return this is ExposedSQLException
}

fun ExposedSQLException.isCausedByDuplicateError(): Boolean {
    return if (this.cause is SQLIntegrityConstraintViolationException) {
        return this.errorCode == 1062
    } else false
}


