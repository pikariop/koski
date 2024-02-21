package fi.oph.koski.kyselyt

import com.typesafe.config.Config
import fi.oph.koski.config.Environment
import fi.oph.koski.log.Logging
import software.amazon.awssdk.auth.credentials.{AwsSessionCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{CreateBucketRequest, GetObjectRequest, GetUrlRequest, PutObjectRequest}
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest

import java.io.InputStream
import java.net.URI
import java.time.Duration
import java.util.UUID
import scala.jdk.CollectionConverters._

class KyselyTulosRepository(config: Config) extends Logging {
  val useAWS = Environment.isServerEnvironment(config)
  lazy val region: Region = Region.of(config.getString("kyselyt.s3.region"))
  lazy val bucketName: String = config.getString("kyselyt.s3.bucket")
  lazy val presignDuration: Duration = config.getDuration("kyselyt.s3.presignDuration")
  lazy val endpointOverride: URI = URI.create(config.getString("kyselyt.s3.endpoint"))

  val s3: S3Client = {
    val awsS3 = S3Client.builder().region(region)
    val awsOrLocalS3 = if (useAWS) awsS3 else {
      logger.warn("Using Localstack for S3")
      awsS3
        .endpointOverride(endpointOverride)
        .credentialsProvider(localstackCredentialsProvider)
    }
    awsOrLocalS3.build()
  }

  if (!useAWS) {
    // Bucketin automaattinen luonti ainoastaan Localstackin kanssa
    createBucketIfDoesNotExist
  }

  def putStream[T](queryId: UUID, name: String, inputStream: InputStream, contentLength: Long): String = {
    val key = objectKey(queryId, name)
    val request = PutObjectRequest.builder()
      .bucket(bucketName)
      .key(key)
      .metadata(mapAsJavaMap(Map {
        "query" -> queryId.toString
      }))
      .build()
    val requestBody = RequestBody.fromInputStream(inputStream, contentLength)
    logger.info(s"Put results to S3: ${s3.utilities().getUrl(GetUrlRequest.builder().bucket(bucketName).key(key).build())} $request")
    s3.putObject(request, requestBody)

    key
  }

  def getPresignedDownloadUrl(queryId: UUID, name: String): String = {
    val key = objectKey(queryId, name)
    val awsPresigner = S3Presigner.builder()
    val presigner = (if (useAWS) {
      awsPresigner
    } else {
      awsPresigner
        .endpointOverride(endpointOverride)
        .credentialsProvider(localstackCredentialsProvider)
    }).build()

    val objectRequest = GetObjectRequest.builder()
      .bucket(bucketName)
      .key(key)
      .build()

    val presignRequest = GetObjectPresignRequest.builder()
      .signatureDuration(presignDuration)
      .getObjectRequest(objectRequest)
      .build()

    presigner
      .presignGetObject(presignRequest)
      .url()
      .toExternalForm
  }

  def objectKey(queryId: UUID, name: String): String = s"$queryId/$name"

  private def createBucketIfDoesNotExist =
    if (!s3.listBuckets().buckets().asScala.exists(bucket => bucket.name() == bucketName)) {
      s3.createBucket(CreateBucketRequest.builder().bucket(bucketName).build())
    }

  private lazy val localstackCredentialsProvider = StaticCredentialsProvider.create(localstackCredentials)
  private lazy val localstackCredentials = AwsSessionCredentials.builder()
    .accessKeyId("000000000000")
    .secretAccessKey("1234")
    .sessionToken("1234")
    .build()
}

case class QueryResult(
  name: String,
  content: Stream[Char],
)

class StringInputStream(stream: Stream[Char]) extends InputStream {
  private val iter = stream.iterator
  override def read(): Int = if (iter.hasNext) iter.next else -1
}

object StringInputStream {
  def apply(string: String) = new StringInputStream(string.toStream)
}

class StringListInputStream(stream: Stream[String]) extends InputStream {
  private var stringIterator = stream.iterator
  private var charIterator: Option[Iterator[Char]] = None

  override def read(): Int = {
    if (charIterator.isEmpty && stringIterator.hasNext) {
      charIterator = Some(stringIterator.next.toStream.iterator)
    }
    charIterator.map { iter =>
      if (iter.hasNext) {
        iter.next
      } else {
        charIterator = None
        read()
      }
    }.getOrElse(-1)
  }
}
