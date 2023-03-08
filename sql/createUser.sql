show databases ;

create database board;
create user 'jyuka'@'localhost' identified by '1234';
select `user` from `mysql`.`user`;
show grants for 'jyuka'@'localhost';

# 권한과 해당 권한을 타 유저에게 부여할 수 있는 명령어
grant all on `board`.* to 'jyuka'@'localhost' with grant option;

flush privileges ;