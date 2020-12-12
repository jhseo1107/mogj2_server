package dev.jhseo.mogj.server.procedures

import dev.jhseo.mogj.server.db.ConnectedParentsHouses
import dev.jhseo.mogj.server.db.Houses
import dev.jhseo.mogj.server.db.ParentsHouses
import dev.jhseo.mogj.server.db.Users
import dev.jhseo.mogj.server.model.User
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
    var graph: Array<MutableList<Int>> = Array(transaction{Houses.selectAll().count()}.toInt() + 1) {MutableList(transaction{Users.select{Users.isParent eq true}.count()}.toInt() + 1) {0} }
    var currentConnection: Array<MutableList<Int>> = Array(transaction{Houses.selectAll().count()}.toInt() + 1) {MutableList(transaction{Users.select{Users.isParent eq true}.count()}.toInt() + 1) {0} }
    var vt: Array<Boolean> = Array(transaction{Users.selectAll().count()}.toInt() + 1) {false}
    var conn: Array<Int> = Array(transaction{Users.selectAll().count()}.toInt() + 1) {0} // conn[parentId] -> houseId

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
            }.toMutableList()
        }

        transaction {
            SchemaUtils.drop(ConnectedParentsHouses)
            SchemaUtils.create(ConnectedParentsHouses)
        }

        for (i in 1 until graph.size) {
            currentConnection[i].forEach {
                if (it in graph[i]) graph[i].remove(it)
            }
        }

        for (i in 1 until graph.size) {
            vt = Array(transaction{Users.selectAll().count()}.toInt()) {false}
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
            for(i in 1 until conn.size) {
                if(conn[i] == 0) continue
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
