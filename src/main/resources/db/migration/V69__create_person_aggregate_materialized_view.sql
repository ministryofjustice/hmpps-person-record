BEGIN;
-------------------------------------------------------

DROP MATERIALIZED VIEW IF EXISTS personrecordservice.person_aggregate_data;
CREATE MATERIALIZED VIEW personrecordservice.person_aggregate_data as (
    with person_out AS (
        SELECT
        *
        FROM personrecordservice.person
    ),
    pseudonym_agg AS (
        SELECT
            ps.fk_person_id,
            array_agg(ps.first_name) as first_name_alias_arr,
            array_agg(ps.last_name) as last_name_alias_arr,
            array_agg(ps.date_of_birth) as date_of_birth_alias_arr
        FROM personrecordservice.pseudonym ps
        group by ps.fk_person_id
    ),
    address_agg AS (
        SELECT
            a.fk_person_id,
            array_agg(a.postcode) as postcode_arr
        FROM personrecordservice.address a
        group by a.fk_person_id
    ),
    reference_agg AS (
        SELECT
            r.fk_person_id,
            array_agg(r.identifier_value) FILTER (WHERE identifier_type = 'CRO') as cro_arr,
            array_agg(r.identifier_value) FILTER (WHERE identifier_type = 'PNC') as pnc_arr
        FROM personrecordservice.reference r
        group by r.fk_person_id
    ),
    sentence_info_agg    AS (
        SELECT
            si.fk_person_id,
            array_agg(si.sentence_date) as sentence_date_arr
        FROM personrecordservice.sentence_info si
        group by si.fk_person_id
    )
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
        addr.postcode_arr,
        r.cro_arr,
        r.pnc_arr,
        s.sentence_date_arr,
        p.version
    FROM
        person_out p
    LEFT JOIN
        pseudonym_agg a ON p.id = a.fk_person_id
    LEFT JOIN
        address_agg addr ON p.id = addr.fk_person_id
    LEFT JOIN
        reference_agg r ON p.id = r.fk_person_id
    LEFT JOIN
        sentence_info_agg s ON p.id = s.fk_person_id
    ORDER BY p.id
);

CREATE UNIQUE INDEX idx_person_aggregate_data_id ON personrecordservice.person_aggregate_data (id);

-----------------------------------------------------
COMMIT;