from django.forms import widgets
from rest_framework import serializers
from timelog.models import Activity

class ActivitySerializer(serializers.ModelSerializer):
    class Meta:
        model = Activity
        fields = ('location', 'date', 'startTime', 'stopTime', 'workTime')