package dev.jhseo.mogj.server.model

class User(var id: Long) {
    companion object {
        fun write(kakaoId: Long, nickname: String, avatar: String, password: String) {

        }
    }
}