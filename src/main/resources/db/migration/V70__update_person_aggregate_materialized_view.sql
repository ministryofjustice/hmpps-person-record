BEGIN;
-------------------------------------------------------

DROP MATERIALIZED VIEW IF EXISTS personrecordservice.person_aggregate_data;
CREATE MATERIALIZED VIEW personrecordservice.person_aggregate_data AS (
    with person_in AS (
        SELECT
        *
        FROM person
    ),
    alias_agg AS (
        SELECT
            ps.fk_person_id,
            array_agg(ps.first_name) as first_name_alias_arr,
            array_agg(ps.last_name) as last_name_alias_arr,
            array_agg(ps.date_of_birth) as date_of_birth_alias_arr
        FROM pseudonym ps
        group by ps.fk_person_id
    ),
    person_collected as (
        SELECT
            p.id,
            p.title,
            p.source_system,
            p.first_name,
            p.middle_names,
            p.last_name,
            p.crn,
            p.prison_number,
            p.defendant_id,
            p.master_defendant_id,
            p.birth_place,
            p.birth_country,
            p.nationality,
            p.religion,
            p.sexual_orientation,
            p.date_of_birth,
            p.sex,
            p.ethnicity,
            a.first_name_alias_arr,
            a.last_name_alias_arr,
            a.date_of_birth_alias_arr,
            p.version
        FROM
            person_in p
        LEFT JOIN
            alias_agg a ON p.id = a.fk_person_id
    ),
    person_exploded AS (
        SELECT
            id,
            source_system,
            crn,
            prison_number,
            date_of_birth,
            sex,
            ethnicity,
            first_name,
            middle_names,
            last_name,
            UNNEST(
                CASE
                    WHEN first_name_alias_arr IS NOT NULL
                    AND ARRAY_LENGTH(first_name_alias_arr, 1) >= 1 THEN first_name_alias_arr
                    ELSE ARRAY [NULL]
                END
            ) AS first_name_alias,
            UNNEST(
                CASE
                    WHEN last_name_alias_arr IS NOT NULL
                    AND ARRAY_LENGTH(last_name_alias_arr, 1) >= 1 THEN last_name_alias_arr
                    ELSE ARRAY [NULL]
                END
            ) AS last_name_alias
        FROM
            person_collected
    ),
    person_alias_cleaned AS (
        SELECT
            id,
            source_system,
            crn,
            prison_number,
            date_of_birth,
            sex,
            ethnicity,
            first_name,
            middle_names,
            last_name,
            REPLACE(
                REPLACE(
                    REPLACE(
                        REPLACE(
                            REPLACE(UPPER(first_name_alias), 'MIG_ERROR_', ''),
                            'NO_SHOW_',
                            ''
                        ),
                        'DUPLICATE_',
                        ''
                    ),
                    '-',
                    ' '
                ),
                '''',
                ''
            ) AS first_name_alias,
            REPLACE(
                REPLACE(
                    REPLACE(
                        REPLACE(
                            REPLACE(UPPER(last_name_alias), 'MIG_ERROR_', ''),
                            'NO_SHOW_',
                            ''
                        ),
                        'DUPLICATE_',
                        ''
                    ),
                    '-',
                    ' '
                ),
                '''',
                ''
            ) AS last_name_alias
        FROM
            person_exploded
    ),
    person_alias_agg AS (
        SELECT
            id,
            any_value(source_system) AS source_system,
            any_value(crn) AS crn,
            any_value(prison_number) AS prison_number,
            any_value(date_of_birth) AS date_of_birth,
            any_value(sex) AS sex,
            any_value(ethnicity) AS ethnicity,
            any_value(first_name) AS first_name,
            any_value(middle_names) AS middle_names,
            any_value(last_name) AS last_name,
            array_agg(first_name_alias) AS first_name_alias_arr,
            array_agg(last_name_alias) AS last_name_alias_arr
        FROM
            person_alias_cleaned
        GROUP BY
            id
    ),
    person_first_clean AS (
        SELECT
            id,
            source_system,
            crn,
            prison_number,
            date_of_birth,
            sex,
            ethnicity,
            first_name_alias_arr,
            last_name_alias_arr,
            REPLACE(
                REPLACE(
                    REPLACE(
                        REPLACE(
                            REPLACE(UPPER(first_name), 'MIG_ERROR_', ''),
                            'NO_SHOW_',
                            ''
                        ),
                        'DUPLICATE_',
                        ''
                    ),
                    '-',
                    ' '
                ),
                '''',
                ''
            ) AS first_name,
            REPLACE(
                REPLACE(
                    REPLACE(
                        REPLACE(
                            REPLACE(UPPER(middle_names), 'MIG_ERROR_', ''),
                            'NO_SHOW_',
                            ''
                        ),
                        'DUPLICATE_',
                        ''
                    ),
                    '-',
                    ' '
                ),
                '''',
                ''
            ) AS middle_names,
            REPLACE(
                REPLACE(
                    REPLACE(
                        REPLACE(
                            REPLACE(UPPER(last_name), 'MIG_ERROR_', ''),
                            'NO_SHOW_',
                            ''
                        ),
                        'DUPLICATE_',
                        ''
                    ),
                    '-',
                    ' '
                ),
                '''',
                ''
            ) AS last_name
        FROM
            person_alias_agg
    ),
    person_basic AS (
        SELECT
            id,
            source_system,
            first_name,
            middle_names,
            last_name,
            crn,
            prison_number,
            date_of_birth,
            sex,
            ethnicity,
            first_name_alias_arr,
            last_name_alias_arr,
            ARRAY(
                SELECT
                    x
                FROM
                    UNNEST(
                        string_to_array(
                            CONCAT_WS(' ', first_name, middle_names, last_name),
                            ' '
                        )
                    ) AS x
                WHERE
                    LENGTH(x) >= 3
            ) AS names_split
        FROM
            person_first_clean
    ),
    person_names_extracted AS (
        SELECT
            id,
            source_system,
            first_name,
            middle_names,
            last_name,
            crn,
            prison_number,
            date_of_birth,
            sex,
            ethnicity,
            first_name_alias_arr,
            last_name_alias_arr,
            names_split,
            CASE
                WHEN ARRAY_LENGTH(names_split, 1) >= 1 THEN names_split [1]
                ELSE NULL
            END AS name_1_std,
            CASE
                WHEN ARRAY_LENGTH(names_split, 1) >= 3 THEN names_split [2]
                ELSE NULL
            END AS name_2_std,
            CASE
                WHEN ARRAY_LENGTH(names_split, 1) >= 4 THEN names_split [3]
                ELSE NULL
            END AS name_3_std,
            names_split [ARRAY_LENGTH(names_split, 1)] AS last_name_std
        FROM
            person_basic
    ),
    person_out AS (
        SELECT
            id,
            source_system,
            first_name,
            middle_names,
            last_name,
            crn,
            prison_number,
            date_of_birth,
            sex,
            ethnicity,
            name_1_std,
            name_2_std,
            name_3_std,
            last_name_std,
            array_remove(
                array_remove(
                    ARRAY [name_1_std] || first_name_alias_arr,
                    NULL
                ),
                ''
            ) AS forename_std_arr,
            array_remove(
                array_remove(
                    ARRAY [last_name_std] || last_name_alias_arr,
                    NULL
                ),
                ''
            ) AS last_name_std_arr
        FROM
            person_names_extracted
    ),
    pseudonym_agg AS (
        SELECT
            fk_person_id,
            array_agg(
                REPLACE(
                    REPLACE(
                        REPLACE(
                            REPLACE(
                                REPLACE(UPPER(first_name), 'MIG_ERROR_', ''),
                                'NO_SHOW_',
                                ''
                            ),
                            'DUPLICATE_',
                            ''
                        ),
                        '-',
                        ' '
                    ),
                    '''',
                    ''
                )
            ) as first_name_alias_arr,
            array_agg(
                REPLACE(
                    REPLACE(
                        REPLACE(
                            REPLACE(
                                REPLACE(UPPER(last_name), 'MIG_ERROR_', ''),
                                'NO_SHOW_',
                                ''
                            ),
                            'DUPLICATE_',
                            ''
                        ),
                        '-',
                        ' '
                    ),
                    '''',
                    ''
                )
            ) as last_name_alias_arr,
            array_agg(date_of_birth) as date_of_birth_alias_arr
        FROM
            pseudonym
        group by
            fk_person_id
    ),
    address_cleaned AS (
        SELECT
            fk_person_id,
            REGEXP_REPLACE(
                UPPER(postcode),
                '\s',
                '',
                'g'
            ) as postcode
        FROM
            address
        where
            LENGTH(postcode) >= 3
        order by
            postcode
    ),
    address_with_outcode AS (
        SELECT
            fk_person_id,
            postcode,
            SUBSTRING(
                postcode
                FROM
                    1 FOR LENGTH(postcode) - 3
            ) AS postcode_outcode
        FROM
            address_cleaned
    ),
    address_agg AS (
        SELECT
            fk_person_id,
            array_agg(postcode) FILTER (
                WHERE
                    postcode NOT IN ('NF11NF')
            ) as postcode_arr,
            array_agg(DISTINCT postcode_outcode) FILTER (
                WHERE
                    postcode_outcode NOT IN ('NF1')
            ) as postcode_outcode_arr
        FROM
            address_with_outcode
        group by
            fk_person_id
    ),
    reference_sorted AS (
        SELECT
            fk_person_id,
            UPPER(identifier_type) AS identifier_type,
            UPPER(identifier_value) AS identifier_value
        FROM
            reference
        order by
            identifier_value
    ),
    reference_agg AS (
        SELECT
            fk_person_id,
            array_agg(DISTINCT identifier_value) FILTER (
                WHERE
                    identifier_type = 'CRO'
                    AND identifier_value NOT IN ('000000/00Z')
            ) as cro_arr,
            array_agg(DISTINCT identifier_value) FILTER (
                WHERE
                    identifier_type = 'PNC'
            ) as pnc_arr
        FROM
            reference_sorted
        group by
            fk_person_id
    ),
    sentence_info_agg AS (
        SELECT
            fk_person_id,
            array_agg(sentence_date) FILTER (
                WHERE
                    sentence_date NOT IN ('1970-01-01', '1990-01-01')
            ) as sentence_date_arr
        FROM
            sentence_info
        group by
            fk_person_id
    ),
    joined AS (
        SELECT
            p.id,
            p.source_system,
            p.first_name,
            p.middle_names,
            p.last_name,
            p.crn,
            p.prison_number,
            p.date_of_birth,
            p.sex,
            p.ethnicity,
            p.name_1_std,
            p.name_2_std,
            p.name_3_std,
            p.last_name_std,
            p.forename_std_arr,
            p.last_name_std_arr,
            a.first_name_alias_arr,
            a.last_name_alias_arr,
            array_remove(a.date_of_birth_alias_arr, NULL) AS date_of_birth_alias_arr,
            array_remove(
                array_remove(addr.postcode_arr, NULL),
                ''
            ) AS postcode_arr,
            array_remove(
                array_remove(
                    addr.postcode_outcode_arr,
                    NULL
                ),
                ''
            ) AS postcode_outcode_arr,
            array_remove(
                array_remove(r.cro_arr, NULL),
                ''
            ) AS cro_arr,
            array_remove(
                array_remove(r.pnc_arr, NULL),
                ''
            ) AS pnc_arr,
            s.sentence_date_arr
        FROM
            person_out p
            LEFT JOIN pseudonym_agg a ON p.id = a.fk_person_id
            LEFT JOIN address_agg addr ON p.id = addr.fk_person_id
            LEFT JOIN reference_agg r ON p.id = r.fk_person_id
            LEFT JOIN sentence_info_agg s ON p.id = s.fk_person_id
        ORDER BY
            p.id
    )
    SELECT
        id,
        source_system,
        first_name,
        middle_names,
        last_name,
        crn,
        prison_number,
        date_of_birth,
        sex,
        ethnicity,
        name_1_std,
        name_2_std,
        name_3_std,
        last_name_std,
        ARRAY(SELECT DISTINCT x FROM unnest(forename_std_arr) AS x) AS forename_std_arr,
        ARRAY(SELECT DISTINCT x FROM unnest(last_name_std_arr) AS x) AS last_name_std_arr,
        ARRAY(SELECT DISTINCT x FROM unnest(first_name_alias_arr) AS x) AS first_name_alias_arr,
        ARRAY(SELECT DISTINCT x FROM unnest(last_name_alias_arr) AS x) AS last_name_alias_arr,
        ARRAY(SELECT DISTINCT x FROM unnest(date_of_birth_alias_arr) AS x) AS date_of_birth_alias_arr,
        ARRAY(SELECT DISTINCT x FROM unnest(postcode_arr) AS x) AS postcode_arr,
        ARRAY(SELECT DISTINCT x FROM unnest(postcode_outcode_arr) AS x) AS postcode_outcode_arr,
        ARRAY(SELECT DISTINCT x FROM unnest(cro_arr) AS x) AS cro_arr,
        ARRAY(SELECT DISTINCT x FROM unnest(pnc_arr) AS x) AS pnc_arr,
        ARRAY(SELECT DISTINCT x FROM unnest(sentence_date_arr) AS x) AS sentence_date_arr
    FROM
        joined
);

CREATE UNIQUE INDEX idx_person_aggregate_data_id ON personrecordservice.person_aggregate_data (id);

-----------------------------------------------------
COMMIT;