package no.nav.sikkerhetstjenesten.felles.graphql

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.sikkerhetstjenesten.felles.rest.IrrecoverableRestException
import no.nav.sikkerhetstjenesten.felles.rest.NotFoundRestException
import no.nav.sikkerhetstjenesten.felles.rest.RecoverableRestException
import no.nav.sikkerhetstjenesten.felles.rest.RestConfig
import org.springframework.graphql.client.GraphQlClient
import org.springframework.graphql.client.GraphQlTransportException
import org.springframework.graphql.client.HttpSyncGraphQlClient
import org.springframework.graphql.support.DocumentSource
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.mock.http.client.MockClientHttpRequest
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.ResponseCreator
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withException
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import reactor.core.publisher.Mono
import tools.jackson.databind.json.JsonMapper
import java.net.SocketTimeoutException
import java.net.URI

class AbstractSyncGraphQLClientAdapterTest : BehaviorSpec({
    val uri = URI.create("http://test/graphql")
    val personDokument = $$"query HentPerson($id: ID!) { person(id: $id) { navn alder } }"
    val personerDokument = $$"query HentPersoner($id: ID!) { personer(id: $id) { navn alder } }"
    val mapper = JsonMapper.builder().build()
    lateinit var server: MockRestServiceServer
    lateinit var adapter: TestAdapter

    beforeEach {
        val builder = RestClient.builder().baseUrl(uri.toString())
        server = MockRestServiceServer.bindTo(builder).build()
        val dokumenter = mockk<DocumentSource> {
            every { getDocument("hentPerson") } returns Mono.just(personDokument)
            every { getDocument("hentPersoner") } returns Mono.just(personerDokument)
        }
        val client = HttpSyncGraphQlClient.builder(builder).documentSource(dokumenter).build()
        adapter = TestAdapter(object : RestConfig(uri, "", "test") {}, client)
    }

    afterEach {
        server.verify()
    }

    fun forventRespons(respons: ResponseCreator, dokument: String = personDokument) {
        server.expect(requestTo(uri))
            .andExpect(method(POST))
            .andExpect { request ->
                val body = mapper.readTree((request as MockClientHttpRequest).bodyAsString)
                body["query"].asString() shouldBe dokument
                body["variables"]["id"].asString() shouldBe "123"
            }
            .andRespond(respons)
    }

    fun forventJson(json: String, dokument: String = personDokument) {
        forventRespons(withSuccess(json, APPLICATION_JSON), dokument)
    }

    Given("oppslag av ett objekt") {
        Then("sendes riktig dokument og variabler og objektet dekodes") {
            forventJson("""{"data":{"person":{"navn":"Ada","alder":37}}}""")

            adapter.hentPerson() shouldBe Person("Ada", 37)
        }

        Then("dekodes objektet også når typen oppgis eksplisitt") {
            forventJson("""{"data":{"person":{"navn":"Ada","alder":37}}}""")

            adapter.hentPersonMedType() shouldBe Person("Ada", 37)
        }

        Then("returneres null når feltet er null uten GraphQL-feil") {
            forventJson("""{"data":{"person":null}}""")

            adapter.hentPerson() shouldBe null
        }

        Then("returneres null når feltet mangler") {
            forventJson("""{"data":{"annetFelt":"verdi"}}""")

            adapter.hentPerson() shouldBe null
        }
    }

    Given("oppslag av et påkrevd objekt") {
        Then("returneres objektet når feltet finnes") {
            forventJson("""{"data":{"person":{"navn":"Ada","alder":37}}}""")

            adapter.hentPåkrevdPerson() shouldBe Person("Ada", 37)
        }

        listOf(
            "er null" to """{"data":{"person":null}}""",
            "mangler" to """{"data":{"annetFelt":"verdi"}}""",
        ).forEach { (tilfelle, json) ->
            Then("kastes en uopprettelig feil med felt og URI når feltet $tilfelle") {
                forventJson(json)

                val feil = shouldThrow<IrrecoverableRestException> { adapter.hentPåkrevdPerson() }

                feil.statusCode shouldBe INTERNAL_SERVER_ERROR
                feil.body.detail shouldBe "Fant ikke feltet person i responsen"
                feil.body.properties?.get("uri") shouldBe uri.toString()
            }
        }
    }

    Given("oppslag av en liste") {
        Then("sendes riktig dokument og variabler og alle elementene dekodes til riktig type") {
            forventJson(
                """{"data":{"personer":[{"navn":"Ada","alder":37},{"navn":"Ola","alder":42}]}}""",
                personerDokument,
            )

            adapter.hentPersoner() shouldBe listOf(Person("Ada", 37), Person("Ola", 42))
        }

        Then("returneres en tom liste når feltet er en tom liste") {
            forventJson("""{"data":{"personer":[]}}""", personerDokument)

            adapter.hentPersoner() shouldBe emptyList()
        }

        Then("returneres null når listefeltet er null uten GraphQL-feil") {
            forventJson("""{"data":{"personer":null}}""", personerDokument)

            adapter.hentPersoner() shouldBe null
        }
    }

    val oppslag = listOf(
        Triple("ett objekt", "person", personDokument) to { adapter.hentPerson() },
        Triple("et påkrevd objekt", "person", personDokument) to { adapter.hentPåkrevdPerson() },
        Triple("en liste", "personer", personerDokument) to { adapter.hentPersoner() },
    )

    oppslag.forEach { (oppslag, hent) ->
        val (beskrivelse, felt, dokument) = oppslag
        Given("feil ved oppslag av $beskrivelse") {
            Then("oversettes HTTP-tidsavbrudd til en uopprettelig feil med opprinnelig årsak og URI") {
                forventRespons(withException(SocketTimeoutException("Tidsavbrudd")), dokument)

                val feil = shouldThrow<IrrecoverableRestException> { hent() }

                feil.statusCode shouldBe INTERNAL_SERVER_ERROR
                (feil.cause is ResourceAccessException) shouldBe true
                (feil.cause?.cause is SocketTimeoutException) shouldBe true
                feil.body.properties?.get("uri") shouldBe uri.toString()
            }

            Then("oversettes GraphQL-transportfeil til en gjenopprettelig feil med samme årsak og URI") {
                val client = mockk<GraphQlClient>()
                val request = mockk<GraphQlClient.RequestSpec>()
                val transportfeil = GraphQlTransportException("Transportfeil", RuntimeException(), mockk())
                every { client.documentName(any()) } returns request
                every { request.variables(mapOf("id" to "123")) } returns request
                every { request.executeSync() } throws transportfeil
                adapter = TestAdapter(object : RestConfig(uri, "", "test") {}, client)

                val feil = shouldThrow<RecoverableRestException> { hent() }

                feil.statusCode shouldBe INTERNAL_SERVER_ERROR
                feil.cause shouldBe transportfeil
                feil.body.properties?.get("uri") shouldBe uri.toString()
            }

            Then("oversettes GraphQL-feil for feltet til riktig REST-feil") {
                forventJson(
                    """{"data":{"$felt":null},"errors":[{"message":"Ikke funnet","path":["$felt"],"extensions":{"code":"NOT_FOUND"}}]}""",
                    dokument,
                )

                val feil = shouldThrow<NotFoundRestException> { hent() }

                feil.uri shouldBe uri
            }

            Then("oversettes dekodingsfeil til en uopprettelig feil med årsak og URI") {
                val verdi = """{"navn":"Ada","alder":"ugyldig"}"""
                val data = if (felt == "personer") "[$verdi]" else verdi
                forventJson("""{"data":{"$felt":$data}}""", dokument)

                val feil = shouldThrow<IrrecoverableRestException> { hent() }

                feil.statusCode shouldBe INTERNAL_SERVER_ERROR
                (feil.cause != null) shouldBe true
                feil.body.properties?.get("uri") shouldBe uri.toString()
            }
        }
    }
}) {
    data class Person(val navn: String, val alder: Int)

    private class TestAdapter(cfg: RestConfig, client: GraphQlClient) : AbstractSyncGraphQLClientAdapter(cfg, client) {
        fun hentPerson() = query<Person>("hentPerson" to "person", mapOf("id" to "123"))

        fun hentPersonMedType() = query("hentPerson" to "person", mapOf("id" to "123"), Person::class.java)

        fun hentPåkrevdPerson() = queryRequired<Person>("hentPerson" to "person", mapOf("id" to "123"))

        fun hentPersoner() = queryList("hentPersoner" to "personer", mapOf("id" to "123"), Person::class.java)
    }
}
