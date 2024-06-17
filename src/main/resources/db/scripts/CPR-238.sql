create table if not exists personmatchscore.last_name_frequency (
	id                                      SERIAL      PRIMARY KEY,
	lastname                                TEXT,
    term_frequency                          DECIMAL
);

create table if not exists personmatchscore.first_name_frequency (
	id                                      SERIAL      PRIMARY KEY,
    first_name                              TEXT,
    term_frequency                          DECIMAL
);

create table if not exists personmatchscore.date_of_birth_frequency (
	id                                      SERIAL      PRIMARY KEY,
	date_of_birth                           DATE,
    term_frequency                          DECIMAL
);
