import akka.actor.ActorSystem
import movie.service.tsv.TsvMovieService

object Main extends App {
  implicit val system = ActorSystem("MovieService")
  implicit val ec = system.dispatcher

  val movieService = TsvMovieService("title.basics.tsv", "name.basics.tsv", "title.principals.tsv", "title.episode.tsv")

  val done = movieService.principalsForMovieName("Blacksmith Scene").runForeach(println)

  val done2 = movieService.tvSeriesWithGreatestNumberOfEpisodes().take(10).runForeach(println)

  done
    .flatMap(_ => done2)
    .onComplete(_ => system.terminate())
}
