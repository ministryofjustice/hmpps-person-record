import json
import pandas as pd
from splink.duckdb.duckdb_linker import DuckDBLinker

import os

model_path = os.environ.get('MODEL_PATH', './model.json')

def score(data):

    # Set up DuckDB linker
    linker = DuckDBLinker(
        [data[data['source_dataset'] == data['source_dataset'].unique()[0]],
         data[data['source_dataset'] == data['source_dataset'].unique()[1]]],
        input_table_aliases=[data['source_dataset'].unique()[0], data['source_dataset'].unique()[1]]
    )
    linker.load_settings_from_json(model_path)

    # Make predictions
    json_output = linker.predict().as_pandas_dataframe().to_json()

    # Return
    return json.loads(json_output)
