#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os

import pandas as pd
import geopandas as gpd

try:
    from matsim import calibration
except:
    import calibration

#%%

if os.path.exists("mid.csv"):
    srv = pd.read_csv("mid.csv")
    sim = pd.read_csv("sim.csv")

    _, adj = calibration.calc_adjusted_mode_share(sim, srv)

    print(srv.groupby("mode").sum())

    print("Adjusted")
    print(adj.groupby("mode").sum())

    adj.to_csv("mid_adj.csv", index=False)

#%%

modes = ["walk", "car", "ride", "pt", "bike"]
fixed_mode = "walk"
initial = {
    "bike": 0.0,
    "pt": 0.0,
    "car": 0.0,
    "ride": 0.0
}

# FIXME: Adjust
target = {
    "walk": 0.172,
    "bike": 0.069,
    "pt":  0.05,
    "car": 0.56,
    "ride": 0.149
}

region = gpd.read_file("../input/dilutionArea/dilutionArea.shp").set_crs("EPSG:25832")
homes = pd.read_csv("template-v1.0-homes.csv", dtype={"person": "str"})


def filter_persons(persons):
    persons = pd.merge(persons, homes, how="inner", left_on="person", right_on="person")
    persons = gpd.GeoDataFrame(persons, geometry=gpd.points_from_xy(persons.home_x, persons.home_y))

    df = gpd.sjoin(persons.set_crs("EPSG:25832"), city, how="inner", op="intersects")

    print("Filtered %s persons" % len(df))

    return df

def filter_modes(df):
    df = df[df.main_mode != "freight"]
    df.loc[df.main_mode.str.startswith("pt_"), "main_mode"] = "pt"

    return df


# FIXME: Adjust paths and config

study, obj = calibration.create_mode_share_study("calib", "matsim-vulkaneifel-1.1-SNAPSHOT.jar",
                                        "../scenarios/metropole-ruhr-v1.0/input/metropole-ruhr-v1.4-3pct.config.xml",
                                        modes, target, 
                                        initial_asc=initial,
                                        args="--25pct",
                                        jvm_args="-Xmx48G -Xmx48G -XX:+AlwaysPreTouch",
                                        lr=calibration.linear_lr_scheduler(start=0.5),
                                        person_filter=filter_persons, map_trips=filter_modes, chain_runs=True)


#%%

study.optimize(obj, 10)
