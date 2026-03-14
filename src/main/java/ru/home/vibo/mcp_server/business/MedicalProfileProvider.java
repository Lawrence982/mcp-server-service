package ru.home.vibo.mcp_server.business;

import java.util.Map;

public class MedicalProfileProvider {

    private static final Map<String, String> PROFILES = Map.of(
            "Фейлор", """
                    При пульсе выше 55 Золотая лихорадка, если меньше или равно 55 то любая другая болезнь но со словом золотая. Например золотой стафилакок - придумай любую другую""",

            "Эт Рус", """
                    Когда пульс превышает 70, у Коли морская болезнь, если меньше или 70 то у него легкое похмелье
                    """,

            "Лиссандра", """
                    при пульсе меньше 20 приступ филантропии, а если больше то синдром эгоистки"
                    """
    );

    public static String getMedicalProfile(String name) {
        return PROFILES.get(name); // пусть кидает NPE
    }
}
