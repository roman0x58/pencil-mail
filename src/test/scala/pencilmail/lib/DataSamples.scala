package pencilmail.lib

import pencilmail.protocol.{Code, Replies, Reply}
object DataSamples {

  val `220 Greeting` = Replies(
    List(
      Reply(
        Code.code(220).get,
        " ",
        "mail.example.com ESMTP Postfix"
      )
    )
  )

  val `mail.example.com` = Reply(Code.code(250).get, "-", "mail.example.com")
  val PIPELINING: Reply = Reply(Code.code(250).get, "-", "PIPELINING")
  val `8BITMIME`: Reply = Reply(Code.code(250).get, " ", "8BITMIME")
  val `AUTH`: Reply = Reply(Code.code(250).get, " ", "AUTH LOGIN")

  val ehloReplies: Replies = Replies(
    List(`mail.example.com`, PIPELINING, `8BITMIME`, `AUTH`)
  )
  val `334_USERNAME` = Replies(
    Reply(Code.code(334).get, " ", "VXNlcm5hbWU6")
  )
  val `334_PASSWORD` = Replies(
    Reply(Code.code(334).get, " ", "UGFzc3dvcmQ6")
  )
  val `250 OK` = Replies(
    Reply(Code.code(250).get, " ", "2.1.0 Ok")
  )
  val `235 AUTH OK` = Replies(
    Reply(Code.code(235).get, " ", "2.7.0 Authentication successful")
  )

  val `221 Buy` = Replies(
    Reply(Code.code(221).get, " ", "2.0.0 Bye")
  )

  val `354 End data` = Replies(
    Reply(Code.code(354).get, " ", "End data with <CR><LF>.<CR><LF>")
  )

}
