package ru.orgcom.picky;

public class CardsListItem {
    public String pic="",title="",anons="", question="", wrong="", right="";
    public int id=0, theme=0;

    public CardsListItem(){}

    public CardsListItem(int id, int theme, String pic, String title, String anons, String question, String wrong, String right){
        this.id=id;
        this.theme=theme;
        this.pic=pic;
        this.title=title;
        this.anons=anons;
        this.question=question;
        this.wrong=wrong;
        this.right=right;
    }

    public CardsListItem(int id, int theme, String title){
        this.id=id;
        this.theme=theme;
        this.title=title;
    }
}