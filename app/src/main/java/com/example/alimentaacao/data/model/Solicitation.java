package com.example.alimentaacao.data.model;

import com.google.firebase.firestore.GeoPoint;
import java.util.List;

public class Solicitation {
    public String id;
    public String ongId;
    public String title;
    public List<Item> items;
    public String status; // ABERTA, EM_ANDAMENTO, CONCLUIDA
    public GeoPoint geo;
    public String atendidaPor;
    public long createdAt;

    public static class Item {
        public String nome;
        public int qtd;
        public Item() {}
        public Item(String nome, int qtd) { this.nome = nome; this.qtd = qtd; }
    }

    public Solicitation() {}
}
