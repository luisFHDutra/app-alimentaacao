package com.example.alimentaacao.data.model;

import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.List;

/** Solicitação de alimentos criada por uma ONG. */
public class Solicitation {
    public String id;

    /** Dono do documento: uid da conta (ONG) que criou. Útil para regras de segurança. */
    public String ownerUid;

    /** Se você relaciona a uma ONG específica (id do doc da coleção ongs). Opcional. */
    public String ongId;

    public String title;          // Título da solicitação (usado pelo diálogo)
    public String status;         // "ABERTA", "ATENDIDA", etc.
    public List<Item> items;      // Itens solicitados
    public GeoPoint geo;          // Localização da ONG/solicitação (opcional)

    public String ownerName;
    public String ownerCity;
    public String ownerUf;
    public com.google.firebase.firestore.GeoPoint ownerGeo;

    @ServerTimestamp public Date createdAt;
    @ServerTimestamp public Date updatedAt;

    public Solicitation() {}

    /** Submodelo do item (nome + quantidade). */
    public static class Item {
        public String nome;
        public int qtd;

        public Item() {}
        public Item(String nome, int qtd) {
            this.nome = nome;
            this.qtd = qtd;
        }
    }
}
