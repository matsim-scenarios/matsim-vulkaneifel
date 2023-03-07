library(tidyverse)
library(readxl)

save_plot_as_jpg <- function(plot, name){
  
  ggsave(
    filename = paste0("C:/Users/ACER/Desktop/Uni/VSP/Vulkaneifel v1.1/Plots/",
                      Sys.Date(),"_",name, ".jpg"
    ),
    plot = plot,
    device = "jpg"
  )
  
  plot
}

## import raw data
mid.raw = read_excel("C:/Users/ACER/Desktop/Uni/VSP/Vulkaneifel v1.1/modal_split_mid.xlsx", col_names = F, sheet = "MID Tidy")

mid.1 <- mid.raw %>%
  t() %>%
  as.data.frame() %>%
  select(-V12)

names <- mid.1[1, ] %>% as.vector() %>% unlist()
print(names)

colnames(mid.1) <- names
mid.2 <- mid.1[-1, ]
rownames(mid.2) <- c()

rm(mid.raw, mid.1, names)

mid.3 <- mid.2 %>%
  filter(!is.na(`Anzahl Wege`)) %>%
  filter(!Verkehrsmittel %in% c("keine Angabe", "gesamt") ) %>%
  mutate(`100 km und mehr` = ifelse(`100 km und mehr` == "-", 0, `100 km und mehr`),
         `50 bis unter 100 km` = ifelse(`50 bis unter 100 km` == "-", 0, `50 bis unter 100 km`),
         `Anzahl Wege` = as.character(`Anzahl Wege`),
         `Anzahl Wege` = as.numeric(str_remove(string = `Anzahl Wege`, pattern = "[.]"))
         ) %>%
  pivot_longer(cols = -c(`Anzahl Wege`, Verkehrsmittel), names_to = "distance_group", values_to = "share") %>%
  group_by(Verkehrsmittel) %>%
  mutate(share = as.numeric(share),
         total = sum(share),
         n_trips = share * `Anzahl Wege`) %>%
  select(-c(total, share, `Anzahl Wege`)) %>%
  pivot_wider(names_from = "Verkehrsmittel", values_from = "n_trips") %>%
  mutate(ÖPNV = `ÖPNV (inkl. Taxi, anderes)` + ÖPFV) %>%
  select(-c(`ÖPNV (inkl. Taxi, anderes)`, ÖPFV))

distance.share <- mid.3 %>%
  pivot_longer(cols = -distance_group, names_to = "mode", values_to = "n_trips") %>%
  pivot_wider(names_from = "distance_group", values_from = n_trips) %>%
  mutate("1 bis 5 km" = `1 bis unter 2 km` + `2 bis unter 5 km`,
         "10 bis 50 km" = `10 bis unter 20 km` + `20 bis unter 50 km`,
         "unter 1 km" = `unter 0,5 km` + `0,5 bis unter 1 km`) %>%
  select(-c(`1 bis unter 2 km`, `2 bis unter 5 km`, `10 bis unter 20 km`, `20 bis unter 50 km`, `unter 0,5 km`, `0,5 bis unter 1 km`)) %>%
  pivot_longer(cols = -mode, names_to = "distance_group", values_to = "n_trips") %>%
  group_by(distance_group) %>%
  mutate(total = sum(n_trips)) %>%
  ungroup() %>%
  mutate(share = n_trips / total,
         share_round = round(share, 3)) %>%
  group_by(distance_group) %>%
  mutate(sum_check = sum(share)) %>%
  select(-c(sum_check, share)) %>%
  mutate(distance_group = factor(distance_group, levels = c("unter 1 km", "1 bis 5 km", "5 bis unter 10 km", "10 bis 50 km", "50 bis unter 100 km", "100 km und mehr"))) %>%
  arrange(distance_group)
         
modal.share <- distance.share %>%
  select(-c(share_round, total)) %>%
  group_by(mode) %>%
  summarise(n_trips = sum(n_trips)) %>%
  ungroup() %>%
  mutate(share = n_trips / sum(n_trips),
         share = round(share, 3),
         mode = factor(mode, levels = c("zu Fuß", "Fahrrad", "ÖPNV", "MIV (Mitfahrer)", "MIV (Fahrer)")))

readr::write_csv2(modal.share, file = "C:/Users/ACER/Desktop/Uni/VSP/Vulkaneifel v1.1/data/mode_share.csv")
readr::write_csv2(distance.share, file = "C:/Users/ACER/Desktop/Uni/VSP/Vulkaneifel v1.1/data/distance_share.csv")

plt.1 <- ggplot(distance.share, aes(x = distance_group, y = share_round, fill = mode)) +
  
  geom_col(col = "black", position = "dodge") +
  
  scale_y_continuous(breaks = seq(0, 1, 0.1)) +
  
  labs(x = "Distanzgruppe", y = "Anteil") +
  
  theme_bw() +
  
  theme(legend.position = "bottom", axis.text.x = element_text(angle = 90))

plt.2 <- ggplot(modal.share, aes(x = mode, y = share, fill = mode)) +
  
  geom_col(col = "black") +
  
  geom_text(aes(label = share), vjust = -.5) +
  
  scale_y_continuous(breaks = seq(0, 1, 0.1)) +
  
  labs(x = "Verkehrsmittel", y = "Anteil") +
  
  theme_bw() +
  
  theme(legend.position = "none")
  
save_plot_as_jpg(plt.2, "MiD_Modal_Share")
save_plot_as_jpg(plt.1, "MiD_Distance_Share")

#### analyse of trips per distance group and compare to matsim trips ####
library(sf)
#TRIPS <-  "C:/Users/ACER/Desktop/Uni/VSP/Vulkaneifel v1.1/runs/030.output_trips.csv.gz"
TRIPS <- "C:/Users/ACER/IdeaProjects/matsim-vulkaneifel/output/output-vulkaneifel-v1.1-25pct/vulkaneifel-v1.1-25pct.output_trips.csv.gz"
HOMES <- "C:/Users/ACER/IdeaProjects/matsim-vulkaneifel/input/vulkaneifel-v1.1-homes.csv"
SHP = "C:/Users/ACER/IdeaProjects/matsim-vulkaneifel/scenario/open-vulkaneifel-scenario/vulkaneifel-v1.0-25pct/dilutionArea/dilutionArea.shp"
shp <- st_read(SHP)
trips <- read_csv2(TRIPS)
persons <- read_csv(HOMES, col_types = c("c","d","d"))

label <- unique(distance.share$distance_group) %>% as.character()
breaks <- c(0, 1000, 5000, 10000, 50000, 100000, Inf)

trips.1 <- persons %>%
  st_as_sf(coords = c("home_x", "home_y"), crs = 25832) %>%
  st_filter(shp) %>%
  select(person) %>%
  left_join(trips, by = "person") %>%
  mutate(distance_group = cut(traveled_distance, labels = label, breaks = breaks)) %>%
  filter(!is.na(distance_group))

trips.1 <- trips %>%
  mutate(distance_group = cut(traveled_distance, labels = label, breaks = breaks)) %>%
  filter(!is.na(distance_group))

sim.sum <- trips.1  %>%
  group_by(distance_group) %>%
  summarise(n = n()) %>%
  ungroup() %>%
  mutate(share = n / sum(n),
         src = "sim")

mid.sum <- distance.share %>%
  select(distance_group, n) %>%
  group_by(distance_group) %>%
  summarise(n = sum(n)) %>%
  ungroup() %>%
  mutate(share = n / sum(n),
         src = "mid")

q <- sum(sim.sum$n)/sum(mid.sum$n)

compare <- bind_rows(mid.sum, sim.sum) %>%
  group_by(src) %>%
  select(distance_group, src, share) %>%
  pivot_wider(names_from = "src", values_from = "share") %>%
  left_join(mid.sum, by = "distance_group") %>%
  select(-src)  %>%
  mutate(total_mid_weight = sum(n) * q) %>%
  mutate(trips_suposed = mid * total_mid_weight,
         trips_in_sim = sim * total_mid_weight,
         diff = trips_in_sim - trips_suposed)
