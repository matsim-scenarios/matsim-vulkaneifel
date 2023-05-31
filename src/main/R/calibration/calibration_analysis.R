library(readr)
library(tidyverse)
library(sf)

options(scipen=999)

DISTANCE_SHARE <- "C:/Users/ACER/Desktop/Uni/VSP/Vulkaneifel v1.1/data/distance_share.csv"
RUNS <- "C:/Users/ACER/Desktop/Uni/VSP/Vulkaneifel v1.1/runs/"

runId <- "030"

HOMES <- "C:/Users/ACER/IdeaProjects/matsim-vulkaneifel/input/vulkaneifel-v1.1-homes.csv"

homes <- read_csv(HOMES, col_types = c("c","d","d"))
trips <- read_csv2(file = paste0(RUNS, runId, ".output_trips.csv.gz"))
distance.share <- read_csv2(DISTANCE_SHARE) %>%
  rename("share" = "share_round",
         "n" = "n_trips") %>%
  mutate(src = "MiD") %>%
  select(-total)

shp <- st_read("C:/Users/ACER/IdeaProjects/matsim-vulkaneifel/scenario/open-vulkaneifel-scenario/vulkaneifel-v1.0-25pct/dilutionArea/dilutionArea.shp")

filtered <- homes %>%
  st_as_sf(coords = c("home_x", "home_y"), crs = 25832) %>%
  st_filter(shp) %>%
  select(person) %>%
  left_join(trips, by = "person") %>%
  filter(!is.na(trip_number))

label <- unique(distance.share$distance_group) %>% as.character()
breaks <- c(0, 1000, 5000, 10000, 50000, 100000, Inf)

filtered.1 <- filtered %>%
  transmute(person, mode = longest_distance_mode, traveled_distance) %>%
  mutate(distance_group = cut(traveled_distance, labels = label, breaks = breaks, right = F),
         geometry = NA) %>%
  as_tibble() %>%
  mutate(mode = ifelse(mode == "walk", "zu Fuß",
                       ifelse(mode == "bike", "Fahrrad",
                              ifelse(mode == "car", "MIV (Fahrer)",
                                     ifelse(mode == "ride", "MIV (Mitfahrer)",
                                            ifelse(mode == "pt", "ÖPNV", "ERROR"))))))

sum <- filtered.1  %>%
  group_by(distance_group, mode) %>%
  summarise(n = n()) %>%
  ungroup() %>%
  group_by(distance_group) %>%
  mutate(share = round(n / sum(n),2),
         src = "sim")

compare <- bind_rows(sum, distance.share) %>%
  mutate(distance_group = factor(distance_group, levels = c("unter 1 km", "1 bis 5 km", "5 bis unter 10 km", "10 bis 50 km", "50 bis unter 100 km", "100 km und mehr")))

compare.1 <- compare %>%
  select(-n) %>%
  pivot_wider(names_from = "src", values_from = "share", values_fill = 0.0) %>%
  mutate(diff = sim - MiD)

ggplot(compare, aes(x = distance_group, y = share, fill = mode)) +
  
  geom_col(position = "dodge", color = "black") +
  
  facet_wrap(. ~ src) +
  
  theme(legend.position = "bottom")

ggplot(compare.1, aes(x = distance_group, y = diff, fill = mode)) +
  
  geom_col(position = "dodge", color = "black") +
  
  geom_hline(mapping = aes(yintercept = -0.02), linetype = "dashed") +
  
  geom_hline(mapping = aes(yintercept = 0.02), linetype = "dashed") +
  
  labs(title = paste("Run:", runId)) +
  
  scale_y_continuous(breaks = seq(-1, 1, .1)) +
  
  coord_flip()