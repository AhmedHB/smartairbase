## Training 

````
Do run this command to train the model:

mamba run -n gat-train python sweep_train.py \
  --config config.yml \
  --missions-file generated_missions_100.json \
  --profiles-config training_profiles.yml

```