INSERT INTO tb_account
VALUES (1, '孙悟空', 18),
       (2, '猪八戒', 19),
       (3, '沙和尚', 19),
       (4, '六耳猕猴', 19),
       (5, '王麻子叔叔', 19);


INSERT INTO tb_idcard
VALUES (1,'0001', '内容1'),
       (2,'0002', '内容2'),
       (3,'0003', '内容3'),
       (4,'0004', '内容4'),
       (5,'0005', '内容5');

INSERT INTO tb_idcard_mapping
VALUES (1,1),
       (2,2),
       (3,3),
       (4,4),
       (5,5);




INSERT INTO tb_book
VALUES (1,1,'图书1', '内容1'),
       (2,2,'图书2', '内容2'),
       (3,1,'图书3', '内容2'),
       (4,1,'图书4', '内容2'),
       (5,1,'图书5', '内容2'),
       (6,2,'图书6', '内容2');



INSERT INTO tb_role
VALUES (1,'角色1'),
       (2,'角色2'),
       (3,'角色3');

INSERT INTO tb_role_mapping
VALUES (1,1),
       (1,3),
       (2,1),
       (2,2),
       (2,3);

INSERT INTO tb_menu
VALUES (1,0,'顶级菜单1'),
       (2,0,'顶级菜单2'),
       (3,0,'顶级菜单3'),
       (4,1,'子菜单'),
       (5,1,'子菜单'),
       (6,3,'子菜单'),
       (7,3,'子菜单'),
       (8,3,'子菜单'),
       (9,4,'子菜单'),
       (10,4,'子菜单'),
       (10,5,'子菜单'),
       (10,5,'子菜单'),
       (10,9,'子菜单'),
       (10,9,'子菜单');