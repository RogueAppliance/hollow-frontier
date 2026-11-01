package com.hollowfrontier.content.frontier.bounty;

import java.util.List;

import net.minecraft.util.RandomSource;

// Generates boss names like "Silas 'Rattlesnake' McGraw".
public final class OutlawNames {
    private OutlawNames() {}

    private static final List<String> FIRST = List.of(
            "Silas", "Jebediah", "Cole", "Wyatt", "Eli", "Josiah", "Clay", "Hank", "Virgil", "Ezra",
            "Belle", "Ruby", "Calamity", "Pearl", "Etta", "Mags");
    private static final List<String> NICK = List.of(
            "Rattlesnake", "Black Jack", "Two-Gun", "Dusty", "Coyote", "Iron", "Mad Dog", "Whiskey",
            "Sidewinder", "Deadeye", "Lucky", "Gravedigger", "Brass Knuckle", "Hollow-Eye");
    private static final List<String> LAST = List.of(
            "McGraw", "Callahan", "Hardin", "Doolin", "Slade", "Barlow", "Crane", "Tolliver",
            "Rourke", "Vance", "Kettle", "Blackwood", "Pike", "Sutter");

    public static String random(RandomSource random) {
        return pick(FIRST, random) + " '" + pick(NICK, random) + "' " + pick(LAST, random);
    }

    private static String pick(List<String> list, RandomSource random) {
        return list.get(random.nextInt(list.size()));
    }
}
