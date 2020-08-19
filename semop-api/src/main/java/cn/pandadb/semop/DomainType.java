package cn.pandadb.semop;

public enum DomainType {
    Any("*"),
    String("string"),
    Number("number"),
    BlobAny("blob/*"),
    BlobImage("blob/image"),
    BlobAudio("blob/audio"),
    ;

    private String _name;

    DomainType(String name) {
        _name = name;
    }
}
