package movie.service.tsv

import java.nio.file.Paths

import akka.stream.alpakka.csv.scaladsl.{CsvParsing, CsvToMap}
import akka.stream.scaladsl._
import movie.service._

import scala.collection.mutable

case class TsvMovieService(basicTitlesPath: String, basicNamesPath: String, principalTitlesPath: String, titleEpisodesPath: String) extends MovieService {
  private val basicTitlesMap: Source[Map[String, String], _] = loadTsvMap(basicTitlesPath)
  private val basicNamesMap: Source[Map[String, String], _] = loadTsvMap(basicNamesPath)
  private val principalTitlesMap: Source[Map[String, String], _]  = loadTsvMap(principalTitlesPath)
  private val titleEpisodesMap: Source[Map[String, String], _] = loadTsvMap(titleEpisodesPath)

  private def loadTsvMap(filename: String): Source[Map[String, String], _] = {
    FileIO
      .fromPath(Paths.get(filename))
      .via(CsvParsing.lineScanner(delimiter = '\t'))
      .via(CsvToMap.toMapAsStrings())
  }

  private def principalFromMap(map: Map[String, String]): Principal = {
    Principal(
      map.getOrElse("primaryName", ""),
      map.getOrElse("birthYear", "0").toInt,
      map.get("deathYear").map(_.toInt),
      map.getOrElse("primaryProfession", "").split(",").toList
    )
  }
  private def toTvSerieFromMap(map: Map[String, String]): TvSeries = {
    TvSeries(
      map.getOrElse("originalTitle", ""),
      map.getOrElse("startYear", "0").toInt,
      map.get("endYear").filter(_ != "\\N").map(_.toInt),
      map.getOrElse("genres", "").split(",").toList
    )
  }

  override def principalsForMovieName(name: String): Source[Principal, _] = {
    basicTitlesMap
      .filter(_.get("primaryTitle").contains(name))
      .flatMapConcat(title => principalTitlesMap.filter(principal => principal.get("tconst") == title.get("tconst")))
      .flatMapConcat(principal => basicNamesMap.filter(basicName => basicName.get("nconst") == principal.get("nconst")))
    .map(principalFromMap)
  }

  override def tvSeriesWithGreatestNumberOfEpisodes(): Source[TvSeries, _] = {
    val ord: Ordering[(String, Int)] = (x, y) => x._2.compareTo(y._2)
    val init: mutable.PriorityQueue[(String, Int)] = mutable.PriorityQueue.newBuilder(ord).result()
    titleEpisodesMap
      .groupBy(Int.MaxValue, _.getOrElse("parentTconst", ""))
      .map(e => e.getOrElse("parentTconst", "") -> 1)
      .reduce((l, r) => (l._1, l._2 + r._2))
      .mergeSubstreams
      .fold(init)((acc, i) => acc.addOne(i))
      .map(_.take(100).toList.map(_._1))
      .flatMapConcat(tconsts => basicTitlesMap.filter(title => tconsts.contains(title.get("tconst"))))
      .map(toTvSerieFromMap)
  }
}
