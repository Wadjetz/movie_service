package movie.service

import akka.stream.scaladsl.Source

final case class Principal(
  name: String,
  birthYear: Int,
  deathYear: Option[Int],
  profession: List[String]
)

final case class TvSeries(
  original: String,
  startYear: Int,
  endYear: Option[Int],
  genres: List[String]
)

trait MovieService {
  /*
  * En tant qu'utilisateur je souhaite pouvoir saisir le nom d'un titre et retourner
  * l'ensemble des membres de l’équipe de tournage.
  */
  def principalsForMovieName(name: String): Source[Principal, _]
  /*
  * En tant qu'utilisateur je souhaite pouvoir retourner le titre des 10 séries avec le plus
  * grand nombre d'épisodes.
  */
  def tvSeriesWithGreatestNumberOfEpisodes(): Source[TvSeries, _]
}
