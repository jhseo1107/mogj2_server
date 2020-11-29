package dev.jhseo.mogj.server.procedures

import dev.jhseo.mogj.server.db.ConnectedParentsHouses
import dev.jhseo.mogj.server.db.Houses
import dev.jhseo.mogj.server.db.ParentsHouses
import dev.jhseo.mogj.server.db.Users
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

// Use Bipartite Matching
class ParentMatcher {
    var graph: Array<MutableList<Int>> = emptyArray()
    var currentConnection: Array<List<Int>> = emptyArray()
    var vt: Array<Boolean> = arrayOf(false)
    var conn: Array<Int> = arrayOf(0) // conn[parentId] -> houseId

    fun run() {
        for (i in 1..transaction { Houses.selectAll().count() }.toInt()) {
            graph[i] = transaction {
                ParentsHouses.select { ParentsHouses.houseId eq i }.map { it[ParentsHouses.parentId] }
            }.toMutableList()
        }
        for (i in 1..transaction { Houses.selectAll().count() }.toInt()) {
            currentConnection[i] = transaction {
                ConnectedParentsHouses.select { ConnectedParentsHouses.houseId eq i }
                    .map { it[ConnectedParentsHouses.parentId] }
            }
        }

        transaction {
            SchemaUtils.drop(ConnectedParentsHouses)
            SchemaUtils.create(ConnectedParentsHouses)
        }

        for (i in 1..graph.size) {
            currentConnection[i].forEach {
                if (it in graph[i]) graph[i].remove(it)
            }
        }

        for (i in 1..graph.size) {
            vt = arrayOf(false)
            dfs(i)
        }

        // Match Unmatched Parents
        for (i in transaction { Users.select { Users.isParent eq true }.map { it[Users.id].value } }) {
            if (conn[i] == 0) {
                conn[i] = transaction {
                    ParentsHouses.select { ParentsHouses.parentId eq i }.map { it[ParentsHouses.houseId] }.first()
                }
            }
        }

        transaction {
            SchemaUtils.drop(ConnectedParentsHouses)
            SchemaUtils.create(ConnectedParentsHouses)
            for(i in 1..conn.size) {
                ConnectedParentsHouses.insert {
                    it[houseId] = conn[i]
                    it[parentId] = i
                }
            }
        }
    }

    private fun dfs(node: Int): Boolean {
        val parents = graph[node]
        for (i in parents) {
            if (vt[i]) continue
            vt[i] = true

            if (conn[i] == 0 || dfs(conn[i])) {
                conn[i] = node
                return true
            }
        }
        return false
    }
}

object PrntMtchRunner {
    fun run() {
        GlobalScope.launch {
            val week: Long = 604800000
            delay(week)
            run()
        }
        ParentMatcher().run()
    }
}
