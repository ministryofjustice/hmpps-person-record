import json
import pandas as pd
from splink.duckdb.linker import DuckDBLinker

import os, sys

model_path = os.environ.get('MODEL_PATH', './hmpps_person_record_python/model.json')

def score(input_data):

    data = pd.DataFrame(json.loads(input_data))

    # Set up DuckDB linker
    linker = DuckDBLinker(
        [data[data['source_dataset'] == data['source_dataset'].unique()[0]],
         data[data['source_dataset'] == data['source_dataset'].unique()[1]]],
        input_table_aliases=[data['source_dataset'].unique()[0], data['source_dataset'].unique()[1]]
    )
    linker.load_settings(model_path)

    # Make predictions
    json_output = linker.predict().as_pandas_dataframe().to_json()

    print(json_output)
    # Return
    return json.loads(json_output)

if __name__ == '__main__':
    # takes a single unnamed argument of correctly formatted data as per
    score(sys.argv[1])