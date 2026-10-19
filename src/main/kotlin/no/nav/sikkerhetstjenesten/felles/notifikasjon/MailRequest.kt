package no.nav.sikkerhetstjenesten.felles.notifikasjon

data class MailRequest(val message: MailMessage, val saveToSentItems: Boolean) {
    data class MailMessage(
        val subject: String?,
        val body: MailBody,
        val toRecipients: List<MailRecipient> = emptyList(),
        val ccRecipients: List<MailRecipient> = emptyList(),
        val bccRecipients: List<MailRecipient> = emptyList(),
        val replyTo: List<MailRecipient> = emptyList()) {
        data class MailBody(val contentType: String, val content: String?)
        data class MailRecipient(val emailAddress: MailAddress) {
            data class MailAddress(val address: String)
        }
    }
}