package data

import protocol.{Banner, Campaign, Targeting}

object CampaignData {
  val activeCampaigns = Seq(
    Campaign(
      id        = 1,
      country   = "LT",
      targeting = Targeting(targetedSiteIds = Set("0006a522ce0f4bbbbaa6b3c38cafaa0f")),
      banners   = List(
        Banner(
          id     = 1,
          src    = "https://business.eskimi.com/wp-content/uploads/2020/06/openGraph.jpeg",
          width  = 300,
          height = 250
        )
      ),
      bid       = 5d
    )
  )

}