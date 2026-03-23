-- ----------------------------
-- 家校通知系统数据库表结构
-- ----------------------------
-- 通知主表
-- ----------------------------
drop table if exists notification;
create table notification (
                              notification_id       bigint(20)      not null auto_increment    comment '通知ID',
                              title                 varchar(255)    not null                   comment '通知标题',
                              content               text            default null               comment '通知正文',
                              sender_id             bigint(20)      not null                   comment '发送人ID',
                              sender_name           varchar(100)    not null                   comment '发送人姓名',
                              jump_url              varchar(500)    default null               comment '跳转链接',
                              attachment_urls       text            default null               comment '附件/图片URL列表(JSON格式)',
                              status                char(1)         default '0'                comment '状态（0草稿 1已发布 2已撤回）',
                              reply_deadline        datetime        default null               comment '回复截止时间',
                              create_by             varchar(64)     default ''                 comment '创建者',
                              create_time           datetime                                   comment '创建时间',
                              update_by             varchar(64)     default ''                 comment '更新者',
                              update_time           datetime                                   comment '更新时间',
                              remark                varchar(500)    default null               comment '备注',
                              primary key (notification_id)
) engine=innodb auto_increment=1 comment = '通知主表';

-- ----------------------------
-- 通知接收对象表
-- ----------------------------
drop table if exists notification_receiver;
create table notification_receiver (
                                       receiver_id           bigint(20)      not null auto_increment    comment '接收关系ID',
                                       notification_id       bigint(20)      not null                   comment '通知ID',
                                       receive_type          char(1)         not null                   comment '接收类型（1班级 2学生/家长 3教职员工）',
                                       receive_ids           text            not null                   comment '接收对象ID列表(JSON格式)',
                                       receive_names         text            not null                   comment '接收对象名称列表(JSON格式)',
                                       create_time           datetime                                   comment '创建时间',
                                       primary key (receiver_id)
) engine=innodb auto_increment=1 comment = '通知接收对象表';

-- ----------------------------
-- 通知抄送对象表
-- ----------------------------
drop table if exists notification_cc;
create table notification_cc (
                                 cc_id                 bigint(20)      not null auto_increment    comment '抄送关系ID',
                                 notification_id       bigint(20)      not null                   comment '通知ID',
                                 cc_type               char(1)         not null                   comment '抄送类型（1教职员工 2学校通讯录）',
                                 cc_ids                text            not null                   comment '抄送对象ID列表(JSON格式)',
                                 cc_names              text            not null                   comment '抄送对象名称列表(JSON格式)',
                                 create_time           datetime                                   comment '创建时间',
                                 primary key (cc_id)
) engine=innodb auto_increment=1 comment = '通知抄送对象表';

-- ----------------------------
-- 问题表
-- ----------------------------
drop table if exists notification_question;
create table notification_question (
                                       question_id           bigint(20)      not null auto_increment    comment '问题 ID',
                                       notification_id       bigint(20)      not null                   comment '通知 ID',
                                       parent_question_id    bigint(20)      default null               comment '父问题 ID（用于分支问题，记录上一题的选项继续后指向此题）',
                                       question_title        varchar(500)    not null                   comment '问题标题',
                                       question_type         char(1)         not null                   comment '问题类型（1 单选 2 多选 3 填空 4 附件上传 5 逻辑表单）',
                                       options               text            default null               comment '选项列表 (JSON 格式)
                                                                                - 单选/多选：["选项 1","选项 2",...]
                                                                                - 逻辑表单：存储在 content 字段中，包含完整的问卷结构和子问题列表',
                                       is_required           char(1)         default '0'                comment '是否必答（0 否 1 是）',
                                       sort_order            int(4)          default 0                  comment '排序',
                                       logic_rules           text            default null               comment '跳转逻辑规则 (JSON 格式)',
                                       fill_blanks           text            default null               comment '填空题的填空列表 (JSON 格式)',
                                       correct_answers       text            default null               comment '填空题的正确答案 (JSON 格式)',
                                       content               text            default null               comment '题目内容（富文本/HTML）',
                                       create_time           datetime                                   comment '创建时间',
                                       primary key (question_id)
) engine=innodb auto_increment=1 comment = '通知问题表';

-- ----------------------------
-- 用户通知阅读状态表
-- ----------------------------
drop table if exists user_notification_read;
create table user_notification_read (
                                        read_id               bigint(20)      not null auto_increment    comment '阅读记录ID',
                                        notification_id       bigint(20)      not null                   comment '通知ID',
                                        user_id               bigint(20)      not null                   comment '用户ID',
                                        user_type             char(1)         not null                   comment '用户类型（1学生 2家长 3教师）',
                                        is_read               char(1)         default '0'                comment '是否已读（0未读 1已读）',
                                        read_time             datetime        default null               comment '阅读时间',
                                        reply_status          char(1)         default '0'                comment '回复状态（0未回复 1已回复）',
                                        reply_time            datetime        default null               comment '回复时间',
                                        create_time           datetime                                   comment '创建时间',
                                        primary key (read_id),
                                        unique key uk_notification_user (notification_id, user_id)
) engine=innodb auto_increment=1 comment = '用户通知阅读状态表';

-- ----------------------------
-- 回复答案表
-- ----------------------------
drop table if exists notification_answer;
create table notification_answer (
                                     answer_id             bigint(20)      not null auto_increment    comment '答案ID',
                                     notification_id       bigint(20)      not null                   comment '通知ID',
                                     question_id           bigint(20)      not null                   comment '问题ID',
                                     user_id               bigint(20)      not null                   comment '用户ID',
                                     user_type             char(1)         not null                   comment '用户类型（1学生 2家长 3教师）',
                                     answer_content        text            default null               comment '答案内容',
                                     attachment_urls       text            default null               comment '附件URL列表(JSON格式)',
                                     create_time           datetime                                   comment '创建时间',
                                     primary key (answer_id)
) engine=innodb auto_increment=1 comment = '通知回答表';

-- ----------------------------
-- 初始化数据
-- ----------------------------
-- 示例通知数据
insert into notification values(1, '关于春季运动会的通知', '各位家长同学，我校将于下周五举办春季运动会，请大家准时参加。', 1, '张老师', null, null, '1', '2026-03-15 23:59:59', 'admin', NOW(), '', null, '重要通知');

-- 示例接收对象数据
insert into notification_receiver values(1, 1, '1', '[1,2,3]', '["一年级1班","一年级2班","二年级1班"]', NOW());
insert into notification_receiver values(2, 1, '2', '[101,102,103]', '["张小明家长","李小红家长","王小华家长"]', NOW());

-- 示例抄送对象数据
insert into notification_cc values(1, 1, '1', '[201,202]', '["李主任","王副校长"]', NOW());
insert into notification_cc values(2, 1, '2', '[301]', '["校长办公室"]', NOW());

-- 示例问题数据
-- 单选题：第一个问题
insert into notification_question values(1, 1, null, '您是否支持举办运动会？', '1', '["支持","不支持"]', '1', 1, null, null, null, null, NOW());
-- 第二个问题：多选题
insert into notification_question values(2, 1, null, '请选择您想参加的项目（可多选）', '2', '["跑步","跳远","投掷","其他"]', '0', 2, null, null, null, null, NOW());
-- 第三个问题：填空题
insert into notification_question values(3, 1, null, '请留下您的联系方式', '3', null, '1', 3, null, null, null, null, NOW());