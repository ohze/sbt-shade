import xerial.sbt.Sonatype.GitHubHosting

publishMavenStyle := true

publishTo := sonatypePublishToBundle.value

sonatypeProjectHosting := Some(
  GitHubHosting("ohze", "sbt-shade", "Bùi Việt Thành", "thanhbv@sandinh.net")
)
